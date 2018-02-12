package com.github.mike10004.seleniumhelp;

import com.github.mike10004.seleniumhelp.ChromeWebDriverFactory.CookiePreparer;
import com.github.mike10004.xvfbmanager.XvfbController;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.List;

public class ChromeCookieUsageTest extends CookieUsageTestBase {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @BeforeClass
    public static void setupChromeDriver() {
        UnitTests.setupRecommendedChromeDriver();
    }

    @Override
    protected WebDriverFactory createCookielessWebDriverFactory(XvfbController xvfbController) {
        return ChromeWebDriverFactory.builder()
                .chromeOptions(UnitTests.createChromeOptions())
                .environment(xvfbController::newEnvironment)
                .build();
    }

    @Override
    protected WebDriverFactory createCookiefulWebDriverFactory(XvfbController xvfbController, List<DeserializableCookie> cookiesSetByServer) {
        CookiePreparer cookieImplanter = ChromeWebDriverFactory.makeCookieImplanter(tmp.getRoot().toPath(), () -> cookiesSetByServer);
        return ChromeWebDriverFactory.builder()
                .chromeOptions(UnitTests.createChromeOptions())
                .environment(xvfbController::newEnvironment)
                .cookiePreparer(cookieImplanter)
                .build();
    }

    @Test
    public void testCookieUsage() throws Exception {
        exerciseCookieCapabilities();
    }

}
