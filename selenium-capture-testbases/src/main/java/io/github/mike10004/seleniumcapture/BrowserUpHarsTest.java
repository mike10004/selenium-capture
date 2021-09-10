package io.github.mike10004.seleniumcapture;

import com.browserup.harreader.HarReaderMode;
import com.browserup.harreader.model.Har;
import com.browserup.harreader.model.HarEntry;
import com.browserup.harreader.model.HarLog;
import com.browserup.harreader.model.HarRequest;
import com.browserup.harreader.model.HarResponse;
import com.github.mike10004.seleniumhelp.HarPlus;
import com.github.mike10004.seleniumhelp.TrafficCollector;
import com.github.mike10004.seleniumhelp.TrafficGenerator;
import com.github.mike10004.seleniumhelp.WebDriverFactory;
import com.github.mike10004.seleniumhelp.WebDriverTestParameter;
import com.google.common.net.MediaType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.mike10004.nanochamp.server.NanoControl;
import io.github.mike10004.nanochamp.server.NanoResponse;
import io.github.mike10004.nanochamp.server.NanoServer;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class BrowserUpHarsTest {

    private static final Charset HAR_CHARSET = StandardCharsets.UTF_8;

    private final WebDriverTestParameter webDriverTestParameter;

    public BrowserUpHarsTest(WebDriverTestParameter webDriverTestParameter) {
        this.webDriverTestParameter = webDriverTestParameter;
    }

    protected void setupWebdriver() {
        webDriverTestParameter.doDriverManagerSetup();
    }

    protected abstract WebDriverFactory buildHeadlessFactory();

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void writeHar() throws IOException {
        StringWriter w = new StringWriter(128);
        Har har = newFakeHar();
        BrowserUpHars.writeHar(har, w);
        String harJson = w.toString();
        JsonObject obj = new Gson().fromJson(harJson, JsonObject.class);
        assertTrue("har has log", obj.has("log"));
    }

    private static Har newFakeHar() {
        Har har = new Har();
        HarLog log = new HarLog();
        HarEntry entry = new HarEntry();
        HarRequest request = new HarRequest();
        HarResponse response = new HarResponse();
        entry.setRequest(request);
        entry.setResponse(response);
        entry.setServerIPAddress("127.0.0.1");
        entry.setTime(123);
        log.getEntries().add(entry);
        har.setLog(log);
        return har;
    }

    @Test
    public void writeHar_ioException() throws IOException {
        File outputFile = temporaryFolder.newFile();
        Writer w = new FileWriter(outputFile);
        w.close();
        Har har = newFakeHar();
        try {
            BrowserUpHars.writeHar(har, w);
            fail("this is not how this should go");
        } catch (IOException ignore) {
        }

    }

    /**
     * Checks that har I/O works reasonably well.
     * Fields of the HarTimings object have nanosecond precision at capture time, but
     * the JSON mapper serializes them with milliseconds precision, possibly to avoid overflow somewhere.
     * Instead, we make sure that the requests and responses are all the same.
     * @throws Exception
     */
    @Test
    public void readAndWriteRealHar() throws Exception {
        setupWebdriver();
        checkState(new Har().equals(new Har()));
        Har har = captureHar(buildHeadlessFactory());
        checkState(!har.getLog().getEntries().isEmpty(), "expect nonempty har captured");

        File subdir = temporaryFolder.newFolder();
        File harFile = new File(subdir, "sample.har");
        BrowserUpHars.writeHar(har, harFile, HAR_CHARSET);
        Har deserializedHar = BrowserUpHars.readHar(harFile, HAR_CHARSET, HarReaderMode.STRICT);

        List<HarEntry> entries = har.getLog().getEntries();
        List<HarEntry> dEntries = deserializedHar.getLog().getEntries();
        if (!entries.equals(dEntries)) {
            assertEquals("num entries", entries.size(), dEntries.size());
            for (int i = 0; i < entries.size() ; i++) {
                HarEntry entry = entries.get(i);
                HarEntry dEntry = dEntries.get(i);
                HarRequest request = entry.getRequest();
                HarRequest dRequest = dEntry.getRequest();
                HarResponse response = entry.getResponse();
                HarResponse dResponse = dEntry.getResponse();

                doAssertEquals(String.format("entry %s %s vs %s", i, BrowserUpHars.describe(request), BrowserUpHars.describe(dRequest)), request, dRequest);
                doAssertEquals(String.format("entry %s %s vs %s", i, BrowserUpHars.describe(response), BrowserUpHars.describe(dResponse)), response, dResponse);
            }
        }
    }

    private static <T> void doAssertEquals(String message, T a, T b) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (!Objects.equals(a, b)) {
            System.out.format("expected:%n%s%n", gson.toJson(a));
            System.out.format("  actual:%n%s%n", gson.toJson(b));
        }
        assertEquals(message, a, b);
    }

    private static final String TRANSPARENT_FAVICON_ICO_BASE64 =
            "AAABAAEAEBACAAEAAQCwAAAAFgAAACgAAAAQAAAAIAAAAAEAAQAAAAAAgAAAAAAAAAAAAAAA" +
            "AAAAAAAAAAAAAAAA////AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD//wAA//8AAP//AAD//wAA//8AAP//AAD//wAA" +
            "//8AAP//AAD//wAA//8AAP//AAD//wAA//8AAP//AAD//wAA";

    private static byte[] getFaviconIcoBytes() {
        return java.util.Base64.getDecoder().decode(TRANSPARENT_FAVICON_ICO_BASE64);
    }

    private Har captureHar(WebDriverFactory factory) throws IOException {
        byte[] faviconBytes = getFaviconIcoBytes();
        NanoServer server = NanoServer.builder()
                .getPath("/favicon.ico", s -> NanoResponse.status(200).content(MediaType.ICO, faviconBytes).build())
                .getPath("/", s -> NanoResponse.status(200).htmlUtf8("<!DOCTYPE html><html><head><title>Welcome</title></head><body><h1>Hello, world!</h1></body></html>"))
                .getPath("/text", s -> NanoResponse.status(200).plainTextUtf8("hello, world"))
                .build();
        try (NanoControl serverCtrl = server.startServer()){
            TrafficCollector collector = TrafficCollector.builder(factory).build();
            HarPlus<Void> harPlus = collector.collect(new TrafficGenerator<Void>() {
                @Override
                public Void generate(WebDriver driver) {
                    driver.get(serverCtrl.baseUri().toString());
                    System.out.println(driver.getTitle());
                    driver.get(String.format("%stext", serverCtrl.baseUri()));
                    return (Void) null;
                }
            });
            return harPlus.har;
        }
    }
}