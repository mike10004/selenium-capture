package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.apache.http.client.utils.URIBuilder;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * Interface of a value class that defines a configuration for a session of webdriving.
 */
public interface WebdrivingConfig {

    /**
     * Gets the proxy specifiation, or null if no proxy is to be used.
     * @return  proxy specification
     */
    @Nullable
    ProxySpecification getProxySpecification();

    /**
     * Gets the certificate and key source to be used when proxying HTTPS traffic.
     * @return the certificate and key source
     */
    @Nullable
    CertificateAndKeySource getCertificateAndKeySource();

    static WebdrivingConfig empty() {
        return Builder.EMPTY;
    }

    static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    final class Builder {

        private ProxySpecification proxySpecification;

        private CertificateAndKeySource certificateAndKeySource;

        private Builder() {
        }

        private static final WebdrivingConfig EMPTY = new Builder().build();

        public Builder proxy(ProxySpecification proxySpecification) {
            this.proxySpecification = proxySpecification;
            return this;
        }

        public Builder proxy(URI proxySpecification) {
            return proxy(UriProxySpecification.of(proxySpecification));
        }

        public Builder proxy(HostAndPort proxyAddress) {
            return proxy(proxyAddress, Collections.emptyList());
        }

        public Builder proxy(HostAndPort proxyAddress, List<String> proxyBypasses) {
            try {
                URIBuilder b = new URIBuilder().setHost(proxyAddress.getHost()).setPort(proxyAddress.getPort());
                for (String bypass : proxyBypasses) {
                    b.addParameter(UriProxySpecification.PARAM_BYPASS, bypass);
                }
                return proxy(b.build());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
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
