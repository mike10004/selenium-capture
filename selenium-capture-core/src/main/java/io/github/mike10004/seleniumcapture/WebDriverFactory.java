package io.github.mike10004.seleniumcapture;

import java.io.IOException;

/**
 * Interface defining methods that produce webdriver instances.
 */
public interface WebDriverFactory {

    /**
     * Creates a webdriving session with the given configuration.
     * @param config the configuration
     * @return the session
     * @throws IOException on I/O error
     */
    WebdrivingSession startWebdriving(WebdrivingConfig config) throws IOException;

}
