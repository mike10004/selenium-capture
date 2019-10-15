package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.net.HostAndPort;
import net.lightbody.bmp.mitm.CertificateAndKeySource;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Collections;
import java.util.List;

class WebdrivingConfigs {

    private WebdrivingConfigs() {}

    private static final WebdrivingConfig EMPTY = new Builder().build();

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

        private ProxySpecification proxySpecification;

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
        Builder proxy(ProxySpecification proxySpecification) {
            this.proxySpecification = proxySpecification;
            return this;
        }

        /**
         * @deprecated use {@link UriProxySpecification#of(URI)} and {@link #proxy(ProxySpecification)}
         */
        @Deprecated
        public Builder proxy(URI proxySpecification) {
            return proxy(UriProxySpecification.of(proxySpecification));
        }

        /**
         * @deprecated use {@link ProxySpecification#builder(FullSocketAddress)} and {@link #proxy(ProxySpecification)}
         */
        @Deprecated
        public Builder proxy(HostAndPort proxyAddress) {
            return proxy(proxyAddress, Collections.emptyList());
        }

        /**
         * @deprecated use {@link ProxySpecification#builder(FullSocketAddress)} and {@link #proxy(ProxySpecification)}
         */
        @Deprecated
        Builder proxy(HostAndPort proxyAddress, List<String> proxyBypasses) {
            ProxySpecification ps = ProxySpecification.builder(FullSocketAddress.fromHostAndPort(proxyAddress))
                    .addProxyBypasses(proxyBypasses)
                    .build();
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

            @Nullable
            private final ProxySpecification proxySpecification;

            @Nullable
            private final CertificateAndKeySource certificateAndKeySource;

            public WebdrivingConfigImpl(Builder builder) {
                proxySpecification = builder.proxySpecification;
                certificateAndKeySource = builder.certificateAndKeySource;
            }

            /**
             * Gets the proxy socket address. Null means do not use a proxy.
             * @return the socket address of the proxy
             */
            @Override
            @Nullable
            public ProxySpecification getProxySpecification() {
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
