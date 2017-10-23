package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.core.har.HarContent;
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
                .environment(ChromeWebDriverFactory.createEnvironmentSupplierForDisplay(display))
                .build();
        testTrafficCollector(webDriverFactory);
    }

    @Test
    public void http_headless() throws Exception {
        WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                .headless().build();
        testTrafficCollector(webDriverFactory);
    }

    @Test
    public void http_headless_brotli() throws Exception {
        WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                .headless().build();
        HarContent content = testTrafficCollector(webDriverFactory, new URL("http://httpbin.org/brotli"));
        System.out.println(content.getText());
    }
}