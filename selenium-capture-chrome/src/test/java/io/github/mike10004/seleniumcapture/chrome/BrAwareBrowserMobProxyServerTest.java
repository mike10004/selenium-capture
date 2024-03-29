package io.github.mike10004.seleniumcapture.chrome;

import io.github.mike10004.seleniumcapture.HarPlus;
import io.github.mike10004.seleniumcapture.TrafficCollector;
import io.github.mike10004.seleniumcapture.testing.UnitTests;
import io.github.mike10004.seleniumcapture.WebDriverFactory;
import com.github.mike10004.xvfbtesting.XvfbRule;
import com.browserup.harreader.model.HarEntry;
import com.browserup.harreader.model.HarResponse;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BrAwareBrowserMobProxyServerTest {

    @Rule
    public XvfbRule xvfbRule = UnitTests.xvfbRuleBuilder().build();

    @BeforeClass
    public static void setUpClass() {
        ChromeUnitTests.setupRecommendedChromeDriver();
    }

    @Test
    public void decodeDuringHarCapture() throws Exception {
        WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                .environment(() -> xvfbRule.getController().newEnvironment())
                .configure(ChromeUnitTests.createChromeOptions())
                .configure(o -> o.setAcceptInsecureCerts(true))
                .build();
        TrafficCollector collector = TrafficCollector.builder(webDriverFactory)
                // the collector uses the BrAwareBrowserUpProxyServer by default, so there is no need to specify it here
                .build();
        // TODO set up a local webserver that serves a brotli page instead of hitting this external one
        String brotliUrl = "https://httpbin.org/brotli";
        HarPlus<String> collection = collector.collect(driver -> {
            driver.get(brotliUrl);
            return driver.getPageSource();
        });
        String brotliPageSource = collection.result;
        brotliPageSource = UnitTests.removeHtmlWrapping(brotliPageSource);
        HarResponse response = collection.har.getLog().getEntries().stream()
                .filter(entry -> brotliUrl.equals(entry.getRequest().getUrl()))
                .map(HarEntry::getResponse)
                .findFirst().orElse(null);
        assertNotNull("expect har contains entry with request to " + brotliUrl, response);
        String actualPageSource = response.getContent().getText();
        assertEquals("page source", brotliPageSource.trim(), actualPageSource.trim());
    }

}