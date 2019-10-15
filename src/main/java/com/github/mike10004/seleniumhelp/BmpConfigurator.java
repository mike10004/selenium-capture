package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.littleshoot.proxy.ChainedProxyManager;

import javax.annotation.Nullable;
import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * Interface of a service class that performs configuration operations relating to Browsermob proxy instances.
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

    class BasicBmpConfigurator implements BmpConfigurator {

        @Nullable
        private final URI upstreamProxyUri;

        private BasicBmpConfigurator(@Nullable URI upstreamProxyUri) {
            this.upstreamProxyUri = upstreamProxyUri;
        }

        @Override
        public void configureUpstream(BrowserMobProxy bmp) {
            if (upstreamProxyUri == null) {
                bmp.setChainedProxy(null);
                if (bmp instanceof BrowserMobProxyServer) {
                    ((BrowserMobProxyServer) bmp).setChainedProxyManager(null);
                }
            } else {
                ProxySpecification proxySpecification = UriProxySpecification.of(upstreamProxyUri);
                ChainedProxyManager chainedProxyManager = proxySpecification.toUpstreamProxy();
                ((BrowserMobProxyServer)bmp).setChainedProxyManager(chainedProxyManager);
            }
        }

        @Override
        public WebdrivingConfig createWebdrivingConfig(BrowserMobProxy bmp, @Nullable CertificateAndKeySource certificateAndKeySource) {
            return WebdrivingConfig.builder()
                    .proxy(BrowserMobs.getConnectableSocketAddress(bmp))
                    .certificateAndKeySource(certificateAndKeySource)
                    .build();
        }

        @Override
        public String toString() {
            MoreObjects.ToStringHelper h = MoreObjects.toStringHelper(this);
            if (upstreamProxyUri != null) h.add("upstreamProxyUri", upstreamProxyUri);
            return h.toString();
        }
    }

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
