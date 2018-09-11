package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.mitm.CertificateAndKeySource;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.List;

public interface WebDriverConfig {

    /**
     * Gets the proxy socket address. Null means do not use a proxy.
     * @return the socket address of the proxy
     */
    @Nullable
    InetSocketAddress getProxyAddress();

    /**
     * Gets the certificate and key source to be used when proxying HTTPS traffic.
     * @return the certificate and key source
     */
    @Nullable
    CertificateAndKeySource getCertificateAndKeySource();

    static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    final class Builder {
        private InetSocketAddress proxyAddress;
        private CertificateAndKeySource certificateAndKeySource;

        private Builder() {
        }

        public Builder proxy(InetSocketAddress proxyAddress) {
            this.proxyAddress = proxyAddress;
            return this;
        }

        public Builder certificateAndKeySource(CertificateAndKeySource certificateAndKeySource) {
            this.certificateAndKeySource = certificateAndKeySource;
            return this;
        }

        public Builder bypassHost(String hostPattern) {
            return this;
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        public Builder bypassHosts(List<String> hostPatterns) {
            hostPatterns.forEach(this::bypassHost);
            return this;
        }

        public WebDriverConfig build() {
            return new WebDriverConfigImpl(this);
        }

        private static class WebDriverConfigImpl implements WebDriverConfig {

            @Nullable
            private final InetSocketAddress proxyAddress;

            @Nullable
            private final CertificateAndKeySource certificateAndKeySource;

            private WebDriverConfigImpl(Builder builder) {
                proxyAddress = builder.proxyAddress;
                certificateAndKeySource = builder.certificateAndKeySource;
            }

            /**
             * Gets the proxy socket address. Null means do not use a proxy.
             * @return the socket address of the proxy
             */
            @Override
            @Nullable
            public InetSocketAddress getProxyAddress() {
                return proxyAddress;
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

        }
    }
}
