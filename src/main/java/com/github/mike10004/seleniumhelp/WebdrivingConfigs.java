package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.net.HostAndPort;
import net.lightbody.bmp.mitm.CertificateAndKeySource;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

class WebdrivingConfigs {

    private WebdrivingConfigs() {}

    private static final WebdrivingConfig EMPTY = new WebdrivingConfig() {

        @Override
        public WebdrivingProxyDefinition getProxySpecification() {
            return NoProxySpecification.getInstance().asWebdrivingProxyDefinition();
        }

        @Nullable
        @Override
        public CertificateAndKeySource getCertificateAndKeySource() {
            return null;
        }

        @Override
        public String toString() {
            return "WebdrivingConfig{INACTIVE}";
        }
    };

    /**
     * Returns an immutable empty config instance.
     * @return an empty config instance.
     */
    static WebdrivingConfig empty() {
        return EMPTY;
    }

    /**
     * Returns a new builder of a config instance.
     * @return a new builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     *
     */
    final static class Builder {

        private WebdrivingProxyDefinition proxySpecification;

        private CertificateAndKeySource certificateAndKeySource;

        private Builder() {
        }

        /**
         * Sets the proxy through which the webdriven browser will send network requests.
         * This is normally used internally to specify the intercepting proxy that will
         * capture traffic.
         * @param proxySpecification
         * @return
         */
        Builder proxy(WebdrivingProxyDefinition proxySpecification) {
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
        Builder proxy(HostAndPort proxyAddress, List<String> proxyBypasses) {
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
