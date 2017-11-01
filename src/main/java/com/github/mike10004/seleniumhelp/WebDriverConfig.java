package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.mitm.CertificateAndKeySource;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;

public class WebDriverConfig {

    @Nullable
    private final InetSocketAddress proxyAddress;

    @Nullable
    private final CertificateAndKeySource certificateAndKeySource;

    private WebDriverConfig(Builder builder) {
        proxyAddress = builder.proxyAddress;
        certificateAndKeySource = builder.certificateAndKeySource;
    }

    /**
     * Gets the proxy socket address. Null means do not use a proxy.
     * @return the socket address of the proxy
     */
    @Nullable
    public InetSocketAddress getProxyAddress() {
        return proxyAddress;
    }

    /**
     * Gets the certificate and key source to be used when proxying HTTPS traffic.
     * @return the certificate and key source
     */
    @Nullable
    public CertificateAndKeySource getCertificateAndKeySource() {
        return certificateAndKeySource;
    }

    public static Builder builder() {
        return new Builder();
    }


    public static final class Builder {
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

        public WebDriverConfig build() {
            return new WebDriverConfig(this);
        }
    }
}
