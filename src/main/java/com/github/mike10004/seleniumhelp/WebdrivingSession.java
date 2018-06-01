package com.github.mike10004.seleniumhelp;

import org.openqa.selenium.WebDriver;

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
                driver.quit();
            }
        };
    }
}
