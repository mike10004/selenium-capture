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
            UnitTests.setupRecommendedChromeDriver();
        }
    }

    @Test
    public void https() throws Exception {
        String display = xvfb.getController().getDisplay();
        WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                .chromeOptions(UnitTests.createChromeOptions())
                .environment(ChromeWebDriverFactory.createEnvironmentSupplierForDisplay(display))
                .build();
        testTrafficCollector(webDriverFactory);
    }

}