package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarResponse;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BrAwareBrowserMobProxyServerTest {

    @BeforeClass
    public static void setUpClass() {
        UnitTests.setupRecommendedChromeDriver();
    }

    @Test
    public void decodeDuringHarCapture() throws Exception {
        TrafficCollector collector = TrafficCollector.builder(new ChromeWebDriverFactory()).build();
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