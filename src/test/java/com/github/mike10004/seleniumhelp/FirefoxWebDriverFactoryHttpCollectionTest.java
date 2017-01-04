package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.bonigarcia.wdm.FirefoxDriverManager;
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
            FirefoxDriverManager.getInstance().setup();
        }
    }

    @Test
    public void testTrafficCollectorWithFirefoxFactory_http() throws Exception {
        String display = xvfb.getController().getDisplay();
        WebDriverFactory webDriverFactory = new FirefoxWebDriverFactory(FirefoxWebDriverFactory.createEnvironmentSupplierForDisplay(display), ImmutableMap.of(), ImmutableList.of());
        testTrafficCollector(webDriverFactory);
    }

}