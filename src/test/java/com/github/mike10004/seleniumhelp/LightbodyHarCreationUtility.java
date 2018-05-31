package com.github.mike10004.seleniumhelp;

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

import static java.nio.charset.StandardCharsets.UTF_8;

public class LightbodyHarCreationUtility {

    private final UtilityConfig config;

    public LightbodyHarCreationUtility(UtilityConfig config) {
        this.config = config;
    }

    public static void main(String[] args) throws Exception {
        File harCreationConfigFile = new File(System.getProperty("user.dir"), "har-creation-config.json");
        UtilityConfig config;
        try (Reader reader = new InputStreamReader(new FileInputStream(harCreationConfigFile), UTF_8)) {
            config = new Gson().fromJson(reader, UtilityConfig.class);
        } catch (FileNotFoundException ignore) {
            config = UtilityConfig.getDefault();
        }
        File harFile = new LightbodyHarCreationUtility(config).createHar();
        System.out.format("%s written (%d bytes)%n", harFile, harFile.length());
    }

    private enum BrowserBrand {
        chrome, firefox
    }


    private static class UtilityConfig {
        public BrowserBrand browserBrand;
        public String scratchDirPathname;
        public static UtilityConfig getDefault() {
            UtilityConfig config = new UtilityConfig();
            config.browserBrand = BrowserBrand.chrome;
            config.scratchDirPathname = FileUtils.getTempDirectoryPath();
            return config;
        }
    }

    protected WebDriverFactory createWebDriverFactory() {
        switch (config.browserBrand) {
            case chrome:
                ChromeDriverManager.getInstance().setup();
                return ChromeWebDriverFactory.builder().build();
            case firefox:
                FirefoxDriverManager.getInstance().setup();
                return FirefoxWebDriverFactory.builder().build();
            default:
                throw new IllegalArgumentException("unrecognized browser brand: " + config.browserBrand);
        }
    }

    public File createHar() throws Exception {
        WebDriverFactory webDriverFactory = createWebDriverFactory();
        File scratchDir = new File(config.scratchDirPathname);
        //noinspection ResultOfMethodCallIgnored
        scratchDir.mkdirs();
        TrafficCollector collector = TrafficCollector.builder(webDriverFactory)
                .collectHttps(new AutoCertificateAndKeySource(scratchDir.toPath()))
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

        public static final String STOP_TEXT = "stop";

        private final CharSource signalInputSource;

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
        }

        @Override
        public Void generate(WebDriver driver) throws IOException {
            System.out.format("collecting traffic; enter %s to stop%n", STOP_TEXT);
            try (BufferedReader reader = new BufferedReader(signalInputSource.openStream())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().equalsIgnoreCase(STOP_TEXT)) {
                        break;
                    }
                }
            }
            //noinspection RedundantCast
            return (Void) null;
        }
    }
}
