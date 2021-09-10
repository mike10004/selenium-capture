package io.github.mike10004.seleniumcapture;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.mitm.CertificateAndKeySource;

import javax.annotation.Nullable;

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
    void configureUpstream(BrowserUpProxy proxy);

    /**
     * Creates a set of webdriving configuration values that are appropriate for the upstream proxy configuration.
     * @param bmp the proxy
     * @param certificateAndKeySource the certificate and key source
     * @return a new webdriving config instance
     */
    WebdrivingConfig createWebdrivingConfig(BrowserUpProxy bmp, @Nullable CertificateAndKeySource certificateAndKeySource);

    /**
     * Returns a configurator that configures a direct connection upstream, meaning no proxy is to be used.
     * @return a configurator
     */
    static BmpConfigurator noUpstreamProxy() {
        return new BasicBmpConfigurator(NoProxySpecification.getInstance().asUpstreamProxyDefinition());
    }

    /**
     * Returns a configurator that configures a Browsermob proxy instance to use an upstream proxy.
     * @param upstreamProxyDefinition upstream proxy definition
     * @return a configurator instance
     * @see UriProxySpecification#toUpstreamProxyDefinition()
     */
    static BmpConfigurator usingUpstreamProxy(UpstreamProxyDefinition upstreamProxyDefinition) {
        requireNonNull(upstreamProxyDefinition);
        return new BasicBmpConfigurator(upstreamProxyDefinition);
    }

}
