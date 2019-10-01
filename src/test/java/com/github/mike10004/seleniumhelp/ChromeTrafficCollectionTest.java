package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.core.har.HarContent;
import net.lightbody.bmp.core.har.HarResponse;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.net.URL;

@RunWith(Enclosed.class)
public class ChromeTrafficCollectionTest {

    public static class HttpTest extends CollectionTestBase {

        public HttpTest() {
            super("http");
        }

        @BeforeClass
        public static void setUpDriver() {
            UnitTests.setupRecommendedChromeDriver();
        }

        @org.junit.Ignore(UnitTests.IGNORE_BECAUSE_UPGRADE_INSECURE_REQUESTS_UNAVOIDABLE)
        @Test
        public void http() throws Exception {
            String display = xvfb.getController().getDisplay();
            WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                    .chromeOptions(UnitTests.createChromeOptions())
                    .environment(ChromeWebDriverFactory.createEnvironmentSupplierForDisplay(display))
                    .build();
            testTrafficCollector(webDriverFactory);
        }

        @org.junit.Ignore(UnitTests.IGNORE_BECAUSE_UPGRADE_INSECURE_REQUESTS_UNAVOIDABLE)
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

    public static class HttpsTest extends CollectionTestBase {

        public HttpsTest() {
            super("https");
        }

        @BeforeClass
        public static void setUpDriver() {
            String driverPath = System.getProperty("webdriver.chrome.driver");
            if (driverPath == null) {
                UnitTests.setupRecommendedChromeDriver();
            }
        }

        @Test
        public void https() throws Exception {
            String display = xvfb.getController().getDisplay();
            WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                    .chromeOptions(UnitTests.createChromeOptions())
                    .environment(ChromeWebDriverFactory.createEnvironmentSupplierForDisplay(display))
                    .build();
            testTrafficCollector(webDriverFactory);
        }

    }
}
