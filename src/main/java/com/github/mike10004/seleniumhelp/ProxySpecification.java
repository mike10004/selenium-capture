package com.github.mike10004.seleniumhelp;

import com.google.common.base.Preconditions;
import org.apache.http.client.utils.URIBuilder;

import javax.annotation.Nullable;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public interface ProxySpecification {

    org.openqa.selenium.Proxy createSeleniumProxy();

    @Nullable
    UpstreamProxy toUpstreamProxy();

    static ProxySpecification noProxy() {
        return NoProxySpecification.getInstance();
    }

    static ProxySpecification throughSocketAddress(FullSocketAddress socketAddress) {
        return UriProxySpecification.of(socketAddress.toUri());
    }

    static Builder builder(FullSocketAddress socketAddress) {
        return new Builder(socketAddress);
    }

    class Builder {

        private final FullSocketAddress socketAddress;
        private final List<String> proxyBypasses;

        private Builder(FullSocketAddress socketAddress) {
            this.socketAddress = requireNonNull(socketAddress, "socketAddress");
            proxyBypasses = new ArrayList<>();
        }

        public Builder addProxyBypass(String bypassPattern) {
            requireNonNull(bypassPattern);
            Preconditions.checkArgument(!bypassPattern.trim().isEmpty(), "bypass pattern must be non-whitespace, non-empty string");
            proxyBypasses.add(bypassPattern);
            return this;
        }

        public Builder addProxyBypasses(List<String> bypassPatterns) {
            for (String p : bypassPatterns) {
                addProxyBypass(p);
            }
            return this;
        }

        public ProxySpecification build() {
            try {
                URIBuilder b = new URIBuilder().setHost(socketAddress.getHost()).setPort(socketAddress.getPort());
                for (String bypass : proxyBypasses) {
                    b.addParameter(UriProxySpecification.PARAM_BYPASS, bypass);
                }
                return UriProxySpecification.of(b.build());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}

