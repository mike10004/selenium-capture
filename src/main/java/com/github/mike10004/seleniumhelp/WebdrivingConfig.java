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
     * Gets the proxy specifiation, or null if no proxy is to be used. The proxy host and port
     * must be specified by the URI, and the URI may additionally specify credentials with
     * {@link URI#getUserInfo()} and type, SOCKS or HTTP, with the URI scheme. If the scheme
     * does not specify the type, then it is assumed that the proxy is an HTTP proxy server.
     * Patterns of hosts to bypass may be supplied as values of query parameters with name
     * {@link ProxyUris#PARAM_BYPASS}.
     * @return the socket address of the proxy
     */
    @Nullable
    URI getProxySpecification();

    /**
     * Gets the certificate and key source to be used when proxying HTTPS traffic.
     * @return the certificate and key source
     */
    @Nullable
    CertificateAndKeySource getCertificateAndKeySource();

    static WebdrivingConfig empty() {
        return Builder.EMPTY;
    }

    /**
     * Gets a list of patterns that specify hosts to bypass the proxy for. These are specified
     * as values of query parameters with name {@link ProxyUris#PARAM_BYPASS} in the
     * {@link #getProxySpecification() proxy specification URI}.
     * @return the bypass pattern list
     */
    default List<String> getProxyBypasses() {
        return ProxyUris.getProxyBypassesFromQueryString(getProxySpecification());
    }

    static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    final class Builder {

        private URI proxySpecification;

        private CertificateAndKeySource certificateAndKeySource;

        private Builder() {
        }

        private static final WebdrivingConfig EMPTY = new Builder().build();

        public Builder proxy(URI proxySpecification) {
            this.proxySpecification = proxySpecification;
            return this;
        }

        public Builder proxy(HostAndPort proxyAddress) {
            return proxy(proxyAddress, Collections.emptyList());
        }

        public Builder proxy(HostAndPort proxyAddress, List<String> proxyBypasses) {
            try {
                URIBuilder b = new URIBuilder().setHost(proxyAddress.getHost()).setPort(proxyAddress.getPort());
                for (String bypass : proxyBypasses) {
                    b.addParameter(ProxyUris.PARAM_BYPASS, bypass);
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

        /**
         * @deprecated use {@link #proxy(URI)} or {@link #proxy(HostAndPort, List)} instead
         */
        @Deprecated
        public Builder bypassHost(String hostPattern) {
            checkState(proxySpecification != null, "proxy spec URI must be set before adding bypass host pattern");
            if (hostPattern != null) {
                if (!hostPattern.trim().isEmpty()) {
                    return proxy(ProxyUris.addBypass(proxySpecification, hostPattern));
                }
            }
            return this;
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Deprecated
        public Builder bypassHosts(List<String> hostPatterns) {
            hostPatterns.forEach(this::bypassHost);
            return this;
        }

        public WebdrivingConfig build() {
            return new WebdrivingConfigImpl(this);
        }

        private static class WebdrivingConfigImpl implements WebdrivingConfig {

            @Nullable
            private final URI proxyUri;

            @Nullable
            private final CertificateAndKeySource certificateAndKeySource;

            private final ImmutableList<String> hostBypassPatterns;

            public WebdrivingConfigImpl(Builder builder) {
                proxyUri = builder.proxySpecification;
                certificateAndKeySource = builder.certificateAndKeySource;
                // cache this because it is immutable
                hostBypassPatterns = ImmutableList.copyOf(ProxyUris.getProxyBypassesFromQueryString(proxyUri));
            }

            @Override
            public List<String> getProxyBypasses() {
                return hostBypassPatterns;
            }

            /**
             * Gets the proxy socket address. Null means do not use a proxy.
             * @return the socket address of the proxy
             */
            @Override
            @Nullable
            public URI getProxySpecification() {
                return proxyUri;
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
                if (proxyUri != null) h.add("proxySpecification", proxyUri);
                if (certificateAndKeySource != null) h.add("certificateAndKeySource", certificateAndKeySource);
                return h.toString();
            }
        }

    }
}
