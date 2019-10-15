package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.mitm.CertificateAndKeySource;

import javax.annotation.Nullable;
import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * Interface of a service class that performs configuration operations relating to Browsermob proxy instances.
 * An implementations of this class must provide a {@link WebdrivingConfig} that specifies a given Browsermob
 * proxy instance is the proxy through which the webdriven browser instance sends network requests, and it may
 * also configure the Browsermob proxy server instance to relay requests to an upstream proxy.
 */
interface BmpConfigurator {

    /**
     * Configures the chained proxy manager of a proxy instance.
     * @param proxy the proxy to configure
     */
    void configureUpstream(BrowserMobProxy proxy);

    /**
     * Creates a set of webdriving configuration values that are appropriate for the upstream proxy configuration.
     * @param bmp the proxy
     * @param certificateAndKeySource the certificate and key source
     * @return a new webdriving config instance
     */
    WebdrivingConfig createWebdrivingConfig(BrowserMobProxy bmp, @Nullable CertificateAndKeySource certificateAndKeySource);

    /**
     * Returns a configurator that configures a direct connection upstream, meaning no proxy is to be used.
     * @return a configurator
     */
    static BmpConfigurator noUpstreamProxy() {
        return new BasicBmpConfigurator(null);
    }

    /**
     * Returns a configurator that configures a Browsermob proxy instance to use an upstream proxy specified by a URI.
     * @param proxySpecUri URI that specifies the upstream proxy
     * @return a configurator instance
     */
    static BmpConfigurator usingUpstreamProxy(URI proxySpecUri) {
        requireNonNull(proxySpecUri);
        return new BasicBmpConfigurator(proxySpecUri);
    }

}
