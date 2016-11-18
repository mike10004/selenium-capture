package com.github.mike10004.seleniumhelp;

import org.junit.BeforeClass;
import org.junit.Test;

public class FirefoxWebDriverFactoryHttpCollectionTest extends CollectionTestBase {

    public FirefoxWebDriverFactoryHttpCollectionTest() {
        super("http");
    }

    @BeforeClass
    public static void setUpDriver() {
        String driverPath = System.getProperty("webdriver.gecko.driver");
        if (driverPath == null) {
            io.github.bonigarcia.wdm.MarionetteDriverManager.getInstance().setup();
        }
    }

    @Test
    public void testTrafficCollectorWithFirefoxFactory_http() throws Exception {
        String display = xvfb.getController().getDisplay();
        WebDriverFactory webDriverFactory;
        if (display == null) {
            webDriverFactory = new FirefoxWebDriverFactory();
        } else {
            webDriverFactory = new FirefoxWebDriverFactory(display);
        }
        testTrafficCollector(webDriverFactory);
    }

}