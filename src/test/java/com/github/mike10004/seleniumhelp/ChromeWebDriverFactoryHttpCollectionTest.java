package com.github.mike10004.seleniumhelp;

import io.github.bonigarcia.wdm.ChromeDriverManager;
import org.junit.BeforeClass;
import org.junit.Test;

public class ChromeWebDriverFactoryHttpCollectionTest extends CollectionTestBase {

    public ChromeWebDriverFactoryHttpCollectionTest() {
        super("http");
    }

    @BeforeClass
    public static void setUpDriver() {
        String driverPath = System.getProperty("webdriver.chrome.driver");
        if (driverPath == null) {
            ChromeDriverManager.getInstance().setup(UnitTests.RECOMMENDED_CHROMEDRIVER_VERSION);
        }
    }

    @Test
    public void http() throws Exception {
        String display = xvfb.getController().getDisplay();
        WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                .environment(ChromeWebDriverFactory.createEnvironmentSupplierForDisplay(display))
                .build();
        testTrafficCollector(webDriverFactory);
    }

}