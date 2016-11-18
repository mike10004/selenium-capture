package com.github.mike10004.seleniumhelp;

import org.junit.BeforeClass;
import org.junit.Test;

public class FirefoxWebDriverFactoryHttpsCollectionTest extends CollectionTestBase {

    public FirefoxWebDriverFactoryHttpsCollectionTest() {
        super("https");
    }

    @BeforeClass
    public static void setUpDriver() {
        String driverPath = System.getProperty("webdriver.gecko.driver");
        if (driverPath == null) {
            io.github.bonigarcia.wdm.MarionetteDriverManager.getInstance().setup();
        }
    }

    @Test
    public void testTrafficCollectorWithFirefoxFactory_https() throws Exception {
        WebDriverFactory webDriverFactory = new FirefoxWebDriverFactory();
        testTrafficCollector(webDriverFactory);
    }

}