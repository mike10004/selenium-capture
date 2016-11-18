package com.github.mike10004.seleniumhelp;

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
            io.github.bonigarcia.wdm.ChromeDriverManager.getInstance().setup();
        }
    }

    @Test
    public void testTrafficCollectorWithFirefoxFactory_http() throws Exception {
        String display = xvfb.getController().getDisplay();
        WebDriverFactory webDriverFactory;
        if (display == null) {
            webDriverFactory = new ChromeWebDriverFactory();
        } else {
            webDriverFactory = new ChromeWebDriverFactory(display);
        }
        testTrafficCollector(webDriverFactory);
    }

}