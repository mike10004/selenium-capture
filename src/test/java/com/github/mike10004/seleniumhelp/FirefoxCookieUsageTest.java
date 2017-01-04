package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbmanager.XvfbController;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.bonigarcia.wdm.FirefoxDriverManager;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

public class FirefoxCookieUsageTest extends CookieUsageTestBase {

    @BeforeClass
    public static void setup() {
        FirefoxDriverManager.getInstance().setup();
    }

    @Override
    protected WebDriverFactory createCookielessWebDriverFactory(XvfbController xvfbController) {
        return new FirefoxWebDriverFactory(xvfbController.configureEnvironment(new HashMap<>()), ImmutableMap.of(), ImmutableList.of());
    }

    @Override
    protected WebDriverFactory createCookiefulWebDriverFactory(XvfbController xvfbController, List<DeserializableCookie> cookiesSetByServer) {
        return new FirefoxWebDriverFactory(xvfbController.configureEnvironment(new HashMap<>()), ImmutableMap.of(), cookiesSetByServer);
    }

    @Test
    public void testCookieUsage() throws Exception {
        exerciseCookieCapabilities();
    }
}
