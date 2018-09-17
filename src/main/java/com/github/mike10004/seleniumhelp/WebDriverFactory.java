package com.github.mike10004.seleniumhelp;

import java.io.IOException;

/**
 * Interface defining methods that produce webdriver instances.
 */
public interface WebDriverFactory {

    /**
     * @deprecated use {@link #startWebdriving(WebdrivingConfig)} instead; method has been renamed for clarity
     */
    @Deprecated
    default WebdrivingSession createWebdrivingSession(WebdrivingConfig config) throws IOException {
        return startWebdriving(config);
    }

    /**
     * Creates a webdriving session with the given configuration.
     * @param config the configuration
     * @return the session
     * @throws IOException on I/O error
     */
    WebdrivingSession startWebdriving(WebdrivingConfig config) throws IOException;

}
