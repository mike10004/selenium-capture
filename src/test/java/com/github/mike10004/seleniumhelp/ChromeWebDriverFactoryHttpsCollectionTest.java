package com.github.mike10004.seleniumhelp;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

public class ChromeWebDriverFactoryHttpsCollectionTest extends CollectionTestBase {

    public ChromeWebDriverFactoryHttpsCollectionTest() {
        super("https");
    }

    @BeforeClass
    public static void setUpDriver() {
        String driverPath = System.getProperty("webdriver.chrome.driver");
        if (driverPath == null) {
            io.github.bonigarcia.wdm.ChromeDriverManager.getInstance().setup();
        }
    }

    @Test
    public void testTrafficCollectorWithFirefoxFactory_http() throws Exception {
        String display = xvfb.getController().getDisplay();
        WebDriverFactory webDriverFactory = new ChromeWebDriverFactory(ChromeWebDriverFactory.createEnvironmentSupplierForDisplay(display), new ChromeOptions(), new DesiredCapabilities());
        testTrafficCollector(webDriverFactory);
    }

}