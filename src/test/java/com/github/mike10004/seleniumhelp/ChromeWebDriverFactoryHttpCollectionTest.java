package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.core.har.HarContent;
import net.lightbody.bmp.core.har.HarResponse;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;

public class ChromeWebDriverFactoryHttpCollectionTest extends CollectionTestBase {

    public ChromeWebDriverFactoryHttpCollectionTest() {
        super("http");
    }

    @BeforeClass
    public static void setUpDriver() {
        String driverPath = System.getProperty("webdriver.chrome.driver");
        if (driverPath == null) {
            UnitTests.setupRecommendedChromeDriver();
        }
    }

    @Test
    public void http() throws Exception {
        String display = xvfb.getController().getDisplay();
        WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                .chromeOptions(UnitTests.createChromeOptions())
                .environment(ChromeWebDriverFactory.createEnvironmentSupplierForDisplay(display))
                .build();
        testTrafficCollector(webDriverFactory);
    }

    @Test
    public void http_headless() throws Exception {
        Assume.assumeFalse("headless tests disabled", UnitTests.isHeadlessChromeTestsDisabled());
        WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                .chromeOptions(UnitTests.createChromeOptions())
                .headless()
                .build();
        testTrafficCollector(webDriverFactory);
    }

    @Test
    public void http_headless_brotli() throws Exception {
        Assume.assumeFalse("headless tests disabled", UnitTests.isHeadlessChromeTestsDisabled());
        WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                .chromeOptions(UnitTests.createChromeOptions())
                .headless()
                .build();
        HarResponse response = testTrafficCollector(webDriverFactory, new URL("http://httpbin.org/brotli"));
        HarContent content = response.getContent();
        System.out.println(content.getText());
    }
}