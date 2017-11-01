package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.openqa.selenium.WebDriver;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Interface defining methods that produce webdriver instances.
 */
public interface WebDriverFactory {

    /**
     * Creates a webdriver instance with a given configuration.
     * @param config the configuration
     * @return the webdriver
     */
    WebDriver createWebDriver(WebDriverConfig config) throws IOException;

    /**
     * Creates a webdriver instance configured to use a given proxy.
     * @param proxy the proxy to use
     * @param certificateAndKeySource the certificate and key source, if any
     * @return a webdriver instance
     * @throws IOException if some I/O error occurs; I/O operations such as
     * creating and populating a profile directory may occur when creating
     * a webdriver instance
     * @deprecated implement {@link #createWebDriver(WebDriverConfig)} instead
     */
    @Deprecated
    default WebDriver createWebDriver (BrowserMobProxy proxy, @Nullable CertificateAndKeySource certificateAndKeySource) throws IOException {
        return createWebDriver(WebDriverConfig.builder()
                .proxy(BrowserMobs.getConnectableSocketAddress(proxy))
                .certificateAndKeySource(certificateAndKeySource)
                .build());
    }

}
