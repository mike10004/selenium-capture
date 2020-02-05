package com.github.mike10004.seleniumhelp;

import com.browserup.harreader.model.HarContent;
import com.browserup.harreader.model.HarResponse;
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

        @Test
        public void http() throws Exception {
            testhttp(false);
        }

        @Test
        public void http_headless() throws Exception {
            testhttp(true);
        }

        private void testhttp(boolean headless) throws Exception {
            Assume.assumeFalse("headless tests disabled", headless && UnitTests.isHeadlessChromeTestsDisabled());
            WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                    .headless(headless)
                    .chromeOptions(UnitTests.createChromeOptions())
                    .environment(createEnvironmentSupplierForDisplay(headless))
                    .build();
            testTrafficCollectorOnExampleDotCom(webDriverFactory);
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
            testhttps(false);
        }

        @Test
        public void https_headless() throws Exception {
            testhttps(true);
        }

        private void testhttps(boolean headless) throws Exception {
            Assume.assumeFalse("headless tests disabled", UnitTests.isHeadlessChromeTestsDisabled());
            WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                    .chromeOptions(UnitTests.createChromeOptions())
                    .configure(options -> {
                        options.setAcceptInsecureCerts(true);
                    })
                    .headless()
                    .environment(createEnvironmentSupplierForDisplay(headless))
                    .build();
            testTrafficCollectorOnHttpbin(webDriverFactory);
        }

    }
}
