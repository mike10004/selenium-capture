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
            UnitTests.setupRecommendedGeckoDriver();
        }
    }

    @Test
    public void http() throws Exception {
        String display = xvfb.getController().getDisplay();
        WebDriverFactory webDriverFactory = FirefoxWebDriverFactory.builder()
            .environment(FirefoxWebDriverFactory.createEnvironmentSupplierForDisplay(display))
            .build();
        testTrafficCollector(webDriverFactory);
    }

}