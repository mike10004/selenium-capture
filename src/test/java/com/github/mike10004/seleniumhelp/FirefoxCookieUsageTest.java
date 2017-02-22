package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbmanager.XvfbController;
import io.github.bonigarcia.wdm.FirefoxDriverManager;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class FirefoxCookieUsageTest extends CookieUsageTestBase {

    @BeforeClass
    public static void setup() {
        FirefoxDriverManager.getInstance().setup();
    }

    @Override
    protected WebDriverFactory createCookielessWebDriverFactory(XvfbController xvfbController) {
        return FirefoxWebDriverFactory.builder()
                .environment(xvfbController::newEnvironment)
                .build();
    }

    @Override
    protected WebDriverFactory createCookiefulWebDriverFactory(XvfbController xvfbController, List<DeserializableCookie> cookiesSetByServer) {
        return FirefoxWebDriverFactory.builder()
                .environment(xvfbController::newEnvironment)
                .cookies(cookiesSetByServer)
                .build();
    }

    @Test
    public void testCookieUsage() throws Exception {
        exerciseCookieCapabilities();
    }
}
