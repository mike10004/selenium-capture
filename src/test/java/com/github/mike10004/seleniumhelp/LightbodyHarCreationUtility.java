package com.github.mike10004.seleniumhelp;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.bonigarcia.wdm.FirefoxDriverManager;
import net.lightbody.bmp.core.har.Har;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Program that opens a web browser, records the traffic, and closes upon reading "stop" from standard input.
 * Configure behavior by creating a configuration file in the current directory named "har-creation-config.json",
 * containing JSON structured as specified by {@link LightbodyHarCreationUtility.UtilityConfig}.
 */
public class LightbodyHarCreationUtility {

    private final UtilityConfig config;

    public LightbodyHarCreationUtility(UtilityConfig config) {
        this.config = config;
    }

    public static void main(String[] args) throws Exception {
        File utilityConfigFile = new File(System.getProperty("user.dir"), "har-creation-config.json");
        System.out.format("expect config file at %s%n", utilityConfigFile);
        UtilityConfig config;
        try (Reader reader = new InputStreamReader(new FileInputStream(utilityConfigFile), UTF_8)) {
            config = new Gson().fromJson(reader, UtilityConfig.class);
        } catch (FileNotFoundException ignore) {
            config = new UtilityConfig();
        }
        System.out.println(config);
        config.checkMyself();
        LightbodyHarCreationUtility utility = new LightbodyHarCreationUtility(config);
        File harFile = utility.createHar();
        System.out.format("%s written (%d bytes)%n", harFile, harFile.length());
    }

    private enum BrowserBrand {
        chrome, firefox
    }

    private static class UtilityConfig {

        public static final BrowserBrand DEFAULT_BROWSER = BrowserBrand.chrome;

        public BrowserBrand browserBrand;
        public String scratchDir;
        public String userDataDir;

        public UtilityConfig() {
            browserBrand = DEFAULT_BROWSER;
            scratchDir = FileUtils.getTempDirectoryPath();
        }

        public void checkMyself() {
            checkState(browserBrand != null, "browserBrand must be non-null");
            checkState(!Strings.isNullOrEmpty(scratchDir), "scratchDir must be non-null and nonempty");
        }

        public String toString() {
            return new GsonBuilder().setPrettyPrinting().create().toJson(this);
        }
    }

    private static ChromeOptions toChromeOptions(File scratchDir, UtilityConfig config) throws IOException {
        checkArgument(config.browserBrand == BrowserBrand.chrome, "only applies to BrowserBrand.chrome, not %s", config.browserBrand);
        ChromeOptions options = new ChromeOptions();
        File userDataDir;
        if (config.userDataDir != null) {
            userDataDir = new File(config.userDataDir);
            //noinspection ResultOfMethodCallIgnored
            userDataDir.mkdirs();
            if (!userDataDir.isDirectory()) {
                throw new IOException("could not create directory: " + userDataDir);
            }
        } else {
            userDataDir = java.nio.file.Files.createTempDirectory(scratchDir.toPath(), "chrome-user-data").toFile();
        }
        options.addArguments("--user-data-dir=" + userDataDir.getAbsolutePath());
        return options;
    }

    protected WebDriverFactory createWebDriverFactory(File scratchDir) throws IOException {
        switch (config.browserBrand) {
            case chrome:
                ChromeDriverManager.getInstance().setup();
                return ChromeWebDriverFactory.builder()
                        .chromeOptions(toChromeOptions(scratchDir, config))
                        .build();
            case firefox:
                FirefoxDriverManager.getInstance().setup();
                return FirefoxWebDriverFactory.builder().build();
            default:
                throw new IllegalArgumentException("unrecognized browser brand: " + config.browserBrand);
        }
    }

    public File createHar() throws Exception {
        File scratchDir = new File(config.scratchDir);
        //noinspection ResultOfMethodCallIgnored
        scratchDir.mkdirs();
        WebDriverFactory webDriverFactory = createWebDriverFactory(scratchDir);
        TrafficCollector collector = TrafficCollector.builder(webDriverFactory)
                .collectHttps(new AutoCertificateAndKeySource(scratchDir.toPath()))
                .onException(ExceptionReactor.LOG_AND_SUPPRESS)
                .build();
        Har har = collector.collect(new InteractiveTrafficGenerator()).har;
        File harFile = File.createTempFile("lightbody", ".har", scratchDir);
        har.writeTo(harFile);
        prettify(harFile, Charset.defaultCharset(), UTF_8);
        return harFile;
    }

    @SuppressWarnings("SameParameterValue")
    private static void prettify(File jsonFile, Charset inputCharset, Charset outputCharset) throws IOException {
        JsonElement element;
        try (Reader reader = new InputStreamReader(new FileInputStream(jsonFile), inputCharset)) {
            element = new JsonParser().parse(reader);
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(jsonFile), outputCharset)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(element, writer);
        }
    }

    private static class InteractiveTrafficGenerator implements TrafficGenerator<Void> {

        private final CharSource signalInputSource;
        private final ImmutableList<InputHandler> inputHandlers;

        public InteractiveTrafficGenerator() {
            this(new CharSource() {
                @Override
                public Reader openStream() {
                    return new FilterReader(new InputStreamReader(System.in, Charset.defaultCharset())) {
                        @Override
                        public void close() {
                        }
                    };
                }
            });
        }

        public InteractiveTrafficGenerator(CharSource signalInputSource) {
            this.signalInputSource = signalInputSource;
            this.inputHandlers = ImmutableList.of(new StopHandler());
        }

        @Override
        public Void generate(WebDriver driver) throws IOException {
            System.out.format("collecting traffic; enter %s to stop%n", NextAction.STOP);
            boolean stopHeard = false;
            try (BufferedReader reader = new BufferedReader(signalInputSource.openStream())) {
                String line;
                while (!stopHeard && (line = reader.readLine()) != null) {
                    String input = line.trim();
                    for (InputHandler inputHandler : inputHandlers) {
                        NextAction action = inputHandler.handle(input);
                        if (action == NextAction.STOP) {
                            stopHeard = true;
                            break;
                        }
                    }
                }
            }
            System.out.format("finishing traffic collection (%s)%n", stopHeard ? "due to STOP" : "not sure why");
            //noinspection RedundantCast
            return (Void) null;
        }
    }

    private enum NextAction {
        CONTINUE, STOP
    }

    private interface InputHandler {
        NextAction handle(String input);
    }

    private static class StopHandler implements InputHandler {
        @Override
        public NextAction handle(String input) {
            return NextAction.STOP.name().equalsIgnoreCase(input) ? NextAction.STOP : NextAction.CONTINUE;
        }
    }
}
