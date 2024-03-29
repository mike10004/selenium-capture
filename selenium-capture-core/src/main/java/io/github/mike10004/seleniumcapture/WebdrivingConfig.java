package io.github.mike10004.seleniumcapture;

import com.browserup.bup.mitm.CertificateAndKeySource;
import com.google.common.base.MoreObjects;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Interface of a value class that defines a configuration for a session of webdriving.
 */
public interface WebdrivingConfig {

    /**
     * Gets the proxy definition. If HTTP traffic is to
     * be captured, then this must return the definition of the intercepting proxy through
     * which the webdriven browser shall be configured to send network requests.
     * @return proxy definition
     */
    WebdrivingProxyDefinition getProxySpecification();

    /**
     * Gets the certificate and key source to be used when middlemanning HTTPS traffic.
     * @return the certificate and key source
     */
    @Nullable
    CertificateAndKeySource getCertificateAndKeySource();

    /**
     * Returns an instance that does not configure the webdriving session for traffic capture.
     * No traffic will be captured.
     * @return a non-capturing config instance
     * @deprecated use {@link #nonCapturing()} instead because the name is more descriptive of the purpose
     */
    @Deprecated
    static WebdrivingConfig inactive() {
        return WebdrivingConfigs.empty();
    }

    /**
     * Returns an instance that does not configure the webdriving session for traffic capture.
     * No traffic will be captured.
     * @return a non-capturing config instance
     */
    static WebdrivingConfig nonCapturing() {
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
     * Builder of webdriving config instances.
     */
    final class Builder {

        private WebdrivingProxyDefinition proxySpecification;

        private CertificateAndKeySource certificateAndKeySource;

        private Builder() {
            proxySpecification = ProxyDefinition.direct();
        }

        /**
         * Sets the proxy through which the webdriven browser will send network requests.
         * This is normally used internally to specify the intercepting proxy that will
         * capture traffic.
         * @param proxySpecification
         * @return this builder instance
         */
        public Builder proxy(WebdrivingProxyDefinition proxySpecification) {
            this.proxySpecification = requireNonNull(proxySpecification);
            return this;
        }

        /**
         * Sets the certifiate and key source used by the intercepting proxy to MITM HTTPS traffic.
         * @param certificateAndKeySource the source
         * @return this builder
         */
        public Builder certificateAndKeySource(CertificateAndKeySource certificateAndKeySource) {
            this.certificateAndKeySource = certificateAndKeySource;
            return this;
        }

        /**
         * Builds a config instance.
         * @return a new instance parameterized by this builder
         */
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
             * Gets the proxy definition.
             * @return proxy definition
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
