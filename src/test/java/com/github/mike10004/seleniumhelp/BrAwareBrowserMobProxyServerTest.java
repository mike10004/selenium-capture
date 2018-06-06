package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbtesting.XvfbRule;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarResponse;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BrAwareBrowserMobProxyServerTest {

    @Rule
    public XvfbRule xvfbRule = new XvfbRule();

    @BeforeClass
    public static void setUpClass() {
        UnitTests.setupRecommendedChromeDriver();
    }

    @Test
    public void decodeDuringHarCapture() throws Exception {
        WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                .environment(() -> xvfbRule.getController().newEnvironment())
                .chromeOptions(UnitTests.createChromeOptions())
                .build();
        TrafficCollector collector = TrafficCollector.builder(webDriverFactory)
                // the collector uses the BrAwareBrowserMobProxyServer by default, so there is no need to specify it here
                .build();
        // TODO set up a local webserver that servers a brotli page instead of hitting this external one
        String brotliUrl = "https://tools-7.kxcdn.com/css/all.min.css";
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