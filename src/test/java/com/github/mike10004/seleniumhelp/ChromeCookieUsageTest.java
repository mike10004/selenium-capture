package com.github.mike10004.seleniumhelp;

import com.github.mike10004.chromecookieimplant.ChromeCookieImplanter;
import com.github.mike10004.seleniumhelp.ChromeWebDriverFactory.CookiePreparer;
import com.github.mike10004.xvfbmanager.XvfbController;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URL;
import java.time.Duration;
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
        Duration cookieImplantTimeout = UnitTests.Settings.timeouts().get("chromeCookieImplant", Duration.ofSeconds(10));
        ChromeCookieImplanter implanter = new CustomCookieImplanter(Ints.checkedCast(cookieImplantTimeout.getSeconds()));
        CookiePreparer cookiePreparer = new ChromeCookiePreparer(tmp.getRoot().toPath(), () -> cookiesSetByServer, implanter);
        return ChromeWebDriverFactory.builder()
                .chromeOptions(UnitTests.createChromeOptions())
                .environment(xvfbController::newEnvironment)
                .cookiePreparer(cookiePreparer)
                .build();
    }

    @Test
    public void testCookieUsage() throws Exception {
        exerciseCookieCapabilities();
    }

    private static class CustomCookieImplanter extends ChromeCookieImplanter {
        public CustomCookieImplanter(int outputTimeoutSeconds) {
            super(Resources.asByteSource(getCrxResourceOrDie()), outputTimeoutSeconds, new Gson());
        }

        static final String EXTENSION_RESOURCE_PATH = "/chrome-cookie-implant.crx";

        private static URL getCrxResourceOrDie() throws IllegalStateException {
            URL url = ChromeCookieImplanter.class.getResource(EXTENSION_RESOURCE_PATH);
            if (url == null) {
                throw new IllegalStateException("resource does not exist: classpath:" + EXTENSION_RESOURCE_PATH);
            }
            return url;
        }

    }

}
