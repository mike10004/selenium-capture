package com.github.mike10004.seleniumhelp;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.service.DriverService;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.time.Duration;

import static java.util.Objects.requireNonNull;

/**
 * Interface of an object that represents a webdriving session.
 * Closing a session invokes {@link WebDriver#quit()}.
 */
public interface WebdrivingSession extends java.io.Closeable {

    /**
     * Gets the webdriver in use for this session.
     * @return the webdriver
     */
    WebDriver getWebDriver();

    /**
     * Gets the driver service, if one was used to create the webdriver.
     * @return the driver service
     */
    @Nullable
    DriverService getDriverService();

    /**
     * Tries to make the webdriver quit, imposing a timeout.
     * @param quitTimeout
     * @throws WebdriverQuitException
     */
    void tryQuit(Duration quitTimeout) throws WebdriverQuitException;

    class WebdriverQuitException extends WebDriverException {

        private final WeakReference<WebDriver> webdriver;

        public WebdriverQuitException(WeakReference<WebDriver> webdriver, String message) {
            this(webdriver, message, null);
        }

        public WebdriverQuitException(WeakReference<WebDriver> webdriver, String message, Throwable cause) {
            super(message, cause);
            this.webdriver = requireNonNull(webdriver);
        }

        @Nullable
        public WebDriver getWebDriver() {
            return webdriver.get();
        }
    }
}
