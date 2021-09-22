package io.github.mike10004.seleniumcapture;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.mitm.CertificateAndKeySource;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Interface of a service class that provides configuration data for
 * webdrivers and optionally performs configuration on Browserup proxy server
 * instances.
 *
 * Webdrivers are provided configuration data that specifies a
 * Browserup proxy whose purpose is traffic interception (for
 * monitoring or capture).
 *
 * Browserup proxy server instances are potentially configured
 * to relay requests to an upstream proxy server.
 */
interface InterceptedWebdrivingConfigurator {

    /**
     * Configures the upstream proxy of a proxy server instance.
     * @param proxy the proxy server to configure
     */
    void configureUpstream(BrowserUpProxy proxy);

    /**
     * Creates a webdriving configuration for traffic interception by
     * the given proxy server instance.
     * @param bup the proxy server instance
     * @param certificateAndKeySource the certificate and key source
     * @return a new webdriving config instance
     */
    WebdrivingConfig createWebdrivingConfig(BrowserUpProxy bup,
                                            @Nullable CertificateAndKeySource certificateAndKeySource);

    /**
     * Returns a configurator that configures a direct connection upstream, meaning no upstream proxy is to be used.
     * @return a configurator
     */
    static InterceptedWebdrivingConfigurator noUpstreamProxy() {
        return usingUpstreamProxy(NoProxySpecification.noUpstreamProxyDefinition());
    }

    /**
     * Returns a configurator that configures a Browsermob proxy instance to use an upstream proxy.
     * @param upstreamProxyDefinition upstream proxy definition
     * @return a configurator instance
     * @see UriProxySpecification#toUpstreamProxyDefinition()
     */
    static InterceptedWebdrivingConfigurator usingUpstreamProxy(UpstreamProxyDefinition upstreamProxyDefinition) {
        requireNonNull(upstreamProxyDefinition);
        return new BasicInterceptedWebdrivingConfigurator(upstreamProxyDefinition);
    }

}
