package com.github.mike10004.seleniumhelp;

import org.openqa.selenium.WebDriver;
import org.slf4j.LoggerFactory;

public interface WebdrivingSession extends java.io.Closeable {

    WebDriver getWebDriver();

    static WebdrivingSession simple(WebDriver driver) {
        return new WebdrivingSession() {
            @Override
            public WebDriver getWebDriver() {
                return driver;
            }

            @Override
            public void close() {
                LoggerFactory.getLogger(WebdrivingSession.class).trace("webdriver quitting");
                driver.quit();
            }
        };
    }
}
