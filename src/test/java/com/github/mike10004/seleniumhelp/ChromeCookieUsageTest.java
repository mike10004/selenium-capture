package com.github.mike10004.seleniumhelp;

import com.github.mike10004.seleniumhelp.ChromeWebDriverFactory.CookiePreparer;
import com.github.mike10004.xvfbmanager.XvfbController;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.HashMap;
import java.util.List;

public class ChromeCookieUsageTest extends CookieUsageTestBase {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @BeforeClass
    public static void setupChromeDriver() {
        ChromeDriverManager.getInstance().setup();
    }

    @Override
    protected WebDriverFactory createCookielessWebDriverFactory(XvfbController xvfbController) {
        return new ChromeWebDriverFactory(xvfbController.configureEnvironment(new HashMap<>()), new ChromeOptions(), new DesiredCapabilities());
    }

    @Override
    protected WebDriverFactory createCookiefulWebDriverFactory(XvfbController xvfbController, List<DeserializableCookie> cookiesSetByServer) {
        CookiePreparer cookieImplanter = ChromeWebDriverFactory.makeCookieImplanter(tmp.getRoot().toPath(), () -> cookiesSetByServer);
        return new ChromeWebDriverFactory(xvfbController.configureEnvironment(new HashMap<>()), new ChromeOptions(), new DesiredCapabilities(), cookieImplanter);
    }

    @Test
    public void testCookieUsage() throws Exception {
        exerciseCookieCapabilities();
    }

}
