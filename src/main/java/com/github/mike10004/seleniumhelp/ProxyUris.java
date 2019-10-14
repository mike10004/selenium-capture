package com.github.mike10004.seleniumhelp;

import com.google.common.net.HostAndPort;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.apache.http.client.utils.URIBuilder;
import org.littleshoot.proxy.ChainedProxyManager;
import org.littleshoot.proxy.ChainedProxyType;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class ProxyUris {

    /**
     * Interface of a service class that performs configuration operations relating to proxy instances.
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
        static BmpConfigurator noProxy() {
            return new BmpConfigurator() {
                @Override
                public void configureUpstream(BrowserMobProxy bmp) {
                    bmp.setChainedProxy(null);
                    if (bmp instanceof BrowserMobProxyServer) {
                        ((BrowserMobProxyServer)bmp).setChainedProxyManager(null);
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
                    return "TrafficCollectorProxyConfigurator{UPSTREAM_NOPROXY}";
                }
            };
        }

        /**
         * Returns a configurator that does not act upon a proxy instance.
         * @return a configurator instance
         */
        static BmpConfigurator inoperative() {
            return new BmpConfigurator() {
                @Override
                public void configureUpstream(BrowserMobProxy proxy) {
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
                    return "TrafficCollectorProxyConfigurator{INOPERATIVE}";
                }
            };
        }

        /**
         * Returns a configurator that configures a proxy to use an upstream proxy specified by a URI.
         * @param proxySpecUriProvider the supplier of the URI
         * @return a configurator instance
         */
        static BmpConfigurator upstream(Supplier<URI> proxySpecUriProvider) {
            requireNonNull(proxySpecUriProvider);
            return new BmpConfigurator() {
                @Override
                public void configureUpstream(BrowserMobProxy bmp) {
                    @Nullable URI proxySpecUri = proxySpecUriProvider.get();
                    if (proxySpecUri == null) {
                        noProxy().configureUpstream(bmp);
                    } else {
                        ProxySpecification proxySpecification = UriProxySpecification.of(proxySpecUri);
                        ChainedProxyManager chainedProxyManager = proxySpecification.toUpstreamProxy();
                        ((BrowserMobProxyServer)bmp).setChainedProxyManager(chainedProxyManager);
                    }
                }

                @Override
                public WebdrivingConfig createWebdrivingConfig(BrowserMobProxy bmp, @Nullable CertificateAndKeySource certificateAndKeySource) {
                    @Nullable URI proxySpecUri = proxySpecUriProvider.get();
                    return WebdrivingConfig.builder()
                            .proxy(proxySpecUri)
                            .certificateAndKeySource(certificateAndKeySource)
                            .build();
                }
            };
        }

    }

    @Nullable
    public static URI createSimple(@Nullable HostAndPort socketAddress, ChainedProxyType proxyType) {
        if (socketAddress == null) {
            return null;
        }
        try {
            return new URIBuilder()
                    .setHost(socketAddress.getHost())
                    .setPort(socketAddress.getPort())
                    .setScheme(proxyType.name().toLowerCase())
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
