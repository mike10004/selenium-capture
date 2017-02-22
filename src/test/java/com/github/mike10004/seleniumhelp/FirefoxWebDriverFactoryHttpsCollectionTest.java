package com.github.mike10004.seleniumhelp;

import io.github.bonigarcia.wdm.FirefoxDriverManager;
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
            FirefoxDriverManager.getInstance().setup();
        }
    }

    @Test
    public void testTrafficCollectorWithFirefoxFactory_https() throws Exception {
        String display = xvfb.getController().getDisplay();
        WebDriverFactory webDriverFactory = FirefoxWebDriverFactory.builder()
            .environment(FirefoxWebDriverFactory.createEnvironmentSupplierForDisplay(display))
            .build();
        testTrafficCollector(webDriverFactory);
    }

}