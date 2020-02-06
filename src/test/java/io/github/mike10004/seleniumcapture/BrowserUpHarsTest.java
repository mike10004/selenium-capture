package io.github.mike10004.seleniumcapture;

import com.browserup.harreader.HarReaderMode;
import com.browserup.harreader.model.Har;
import com.browserup.harreader.model.HarEntry;
import com.browserup.harreader.model.HarRequest;
import com.browserup.harreader.model.HarResponse;
import com.github.mike10004.seleniumhelp.FirefoxWebDriverFactory;
import com.github.mike10004.seleniumhelp.HarPlus;
import com.github.mike10004.seleniumhelp.TrafficCollector;
import com.github.mike10004.seleniumhelp.TrafficGenerator;
import com.github.mike10004.seleniumhelp.WebDriverFactory;
import com.google.common.net.MediaType;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.mike10004.nanochamp.server.NanoControl;
import io.github.mike10004.nanochamp.server.NanoResponse;
import io.github.mike10004.nanochamp.server.NanoServer;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.*;

public class BrowserUpHarsTest {

    private static final Charset HAR_CHARSET = StandardCharsets.UTF_8;

    @BeforeClass
    public static void setupGeckodriver() {
        WebDriverManager.firefoxdriver().setup();
    }

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * Checks that har I/O works reasonably well.
     * Fields of the HarTimings object have nanosecond precision at capture time, but
     * the JSON mapper serializes them with milliseconds precision, possibly to avoid overflow somewhere.
     * Instead, we make sure that the requests and responses are all the same.
     * @throws Exception
     */
    @Test
    public void readAndWriteHar() throws Exception {
        checkState(new Har().equals(new Har()));
        Har har = captureHar(FirefoxWebDriverFactory.builder().configure(o -> o.setHeadless(true)).build());
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
                assertEquals(String.format("entry %s %s vs %s", i, BrowserUpHars.describe(request), BrowserUpHars.describe(dRequest)), request, dRequest);
                assertEquals(String.format("entry %s %s vs %s", i, BrowserUpHars.describe(response), BrowserUpHars.describe(dResponse)), response, dResponse);
            }
        }
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