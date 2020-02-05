package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.net.HostAndPort;
import com.browserup.bup.mitm.CertificateAndKeySource;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Interface of a value class that defines a configuration for a session of webdriving.
 */
public interface WebdrivingConfig {

    /**
     * Gets the proxy specifiation, or null if no proxy is to be used. If HTTP traffic is to
     * be captured, then this must return the definition of the intercepting proxy through
     * which the webdriven browser shall be configured to send network requests.
     * @return  proxy specification
     */
    @Nullable
    WebdrivingProxyDefinition getProxySpecification();

    /**
     * Gets the certificate and key source to be used when middlemanning HTTPS traffic.
     * @return the certificate and key source
     */
    @Nullable
    CertificateAndKeySource getCertificateAndKeySource();

    /**
     * Returns a config instance that does not affect the webdriving session.
     * No traffic will be captured.
     * @return a
     */
    static WebdrivingConfig inactive() {
        return WebdrivingConfigs.empty();
    }

    /**
     * @deprecated use {@link #inactive()} instead
     */
    @Deprecated
    static WebdrivingConfig empty() {
        return inactive();
    }

    /**
     * Returns a new builder of a config instance.
     * @return a new builder
     */
    static WebdrivingConfig.Builder builder() {
        return new WebdrivingConfig.Builder();
    }

    /**
     *
     */
    final class Builder {

        private WebdrivingProxyDefinition proxySpecification;

        private CertificateAndKeySource certificateAndKeySource;

        private Builder() {
        }

        /**
         * Sets the proxy through which the webdriven browser will send network requests.
         * This is normally used internally to specify the intercepting proxy that will
         * capture traffic.
         * @param proxySpecification
         * @return this builder instance
         */
        public Builder proxy(WebdrivingProxyDefinition proxySpecification) {
            this.proxySpecification = proxySpecification;
            return this;
        }

        /**
         * @deprecated use {@link ProxyDefinitionBuilder#through(FullSocketAddress)} and {@link #proxy(ProxySpecification)}
         */
        @Deprecated
        public Builder proxy(HostAndPort proxyAddress) {
            return proxy(proxyAddress, Collections.emptyList());
        }

        /**
         * @deprecated use {@link ProxyDefinitionBuilder#through(FullSocketAddress)} and {@link #proxy(ProxySpecification)}
         */
        @Deprecated
        public Builder proxy(HostAndPort proxyAddress, List<String> proxyBypasses) {
            WebdrivingProxyDefinition ps = ProxyDefinitionBuilder.through(FullSocketAddress.fromHostAndPort(proxyAddress))
                    .addProxyBypasses(proxyBypasses)
                    .buildWebdrivingProxyDefinition();
            return proxy(ps);
        }

        public Builder certificateAndKeySource(CertificateAndKeySource certificateAndKeySource) {
            this.certificateAndKeySource = certificateAndKeySource;
            return this;
        }

        public WebdrivingConfig build() {
            return new WebdrivingConfigImpl(this);
        }

        private static class WebdrivingConfigImpl implements WebdrivingConfig {

            private final WebdrivingProxyDefinition proxySpecification;

            @Nullable
            private final CertificateAndKeySource certificateAndKeySource;

            public WebdrivingConfigImpl(Builder builder) {
                proxySpecification = requireNonNull(builder.proxySpecification);
                certificateAndKeySource = builder.certificateAndKeySource;
            }

            /**
             * Gets the proxy socket address. Null means do not use a proxy.
             * @return the socket address of the proxy
             */
            @Override
            public WebdrivingProxyDefinition getProxySpecification() {
                return proxySpecification;
            }

            /**
             * Gets the certificate and key source to be used when proxying HTTPS traffic.
             * @return the certificate and key source
             */
            @Override
            @Nullable
            public CertificateAndKeySource getCertificateAndKeySource() {
                return certificateAndKeySource;
            }


            @Override
            public String toString() {
                MoreObjects.ToStringHelper h = MoreObjects.toStringHelper("WebdrivingSessionConfig");
                if (proxySpecification != null) h.add("proxySpecification", proxySpecification);
                if (certificateAndKeySource != null) h.add("certificateAndKeySource", certificateAndKeySource);
                return h.toString();
            }
        }

    }
}
