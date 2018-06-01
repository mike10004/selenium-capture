package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbtesting.XvfbRule;
import io.github.mike10004.nanochamp.repackaged.fi.iki.elonen.NanoHTTPD;
import io.github.mike10004.nanochamp.repackaged.fi.iki.elonen.NanoHTTPD.Response.Status;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;

import static org.junit.Assert.assertEquals;

public class UnproxiedWebDriverTest {

    private NanoHTTPD nano;

    @Rule
    public XvfbRule xvfbRule = XvfbRule.builder().build();

    @Before
    public void startNanoServer() throws Exception {
        nano = new NanoHTTPD(getUnusedPort()) {
            @Override
            public Response serve(IHTTPSession session) {
                if ("/".equals(URI.create(session.getUri()).getPath())) {
                    return newFixedLengthResponse(Status.OK, "text/plain", "hello");
                }
                return newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "no response defined");
            }
        };
        nano.start();
    }

    @After
    public void stopNanoServer() {
        if (nano != null) {
            nano.stop();
        }
    }

    private int getUnusedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Test
    public void testChrome() throws Exception {
        UnitTests.setupRecommendedChromeDriver();
        ChromeOptions options = UnitTests.createChromeOptions();
        options.addArguments("--headless");
        ChromeWebDriverFactory factory = ChromeWebDriverFactory.builder()
                .chromeOptions(options)
                .build();
        testUnproxied(() -> factory.createWebdrivingSession(EMPTY_WEBDRIVER_CONFIG));
    }

    private static final WebDriverConfig EMPTY_WEBDRIVER_CONFIG = WebDriverConfig.builder().build();

    @Test
    public void testFirefox() throws Exception {
        UnitTests.setupRecommendedGeckoDriver();
        FirefoxWebDriverFactory factory = FirefoxWebDriverFactory.builder().environment(xvfbRule.getController().newEnvironment()).build();
        testUnproxied(() -> factory.createWebdrivingSession(EMPTY_WEBDRIVER_CONFIG));
    }

    private interface ExceptingSupplier<T, X extends Throwable> {
        T get() throws X;
    }

    private void testUnproxied(ExceptingSupplier<? extends WebdrivingSession, ? extends Exception> driverSupplier) throws Exception {
        try (WebdrivingSession session = driverSupplier.get()) {
            WebDriver driver = session.getWebDriver();
            driver.get("http://localhost:" + nano.getListeningPort() + "/");
            String bodyText = driver.findElement(By.tagName("body")).getText();
            System.out.println(StringUtils.abbreviate(bodyText, 256));
            assertEquals("body text", "hello", bodyText);
        }
    }
}
