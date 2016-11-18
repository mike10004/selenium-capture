package com.github.mike10004.seleniumhelp;

import org.junit.BeforeClass;
import org.junit.Test;

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
        WebDriverFactory webDriverFactory = new ChromeWebDriverFactory(xvfb.getController().getDisplay());
        testTrafficCollector(webDriverFactory);
    }

}