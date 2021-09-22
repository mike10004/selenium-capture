package io.github.mike10004.seleniumcapture;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.apache.http.client.utils.URIBuilder;

import javax.annotation.Nullable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class ProxyDefinitionBuilder {

    private final FullSocketAddress socketAddress;
    private final List<String> proxyBypasses;
    @Nullable
    private ProxyProtocol proxyProtocol;

    protected ProxyDefinitionBuilder(FullSocketAddress socketAddress) {
        this.socketAddress = requireNonNull(socketAddress, "socketAddress");
        proxyBypasses = new ArrayList<>();
    }

    /**
     * Designed to align with {@link org.littleshoot.proxy.ChainedProxy}.
     */
    public enum ProxyProtocol {
        http, socks4, socks5;

        String toScheme() {
            return name().toLowerCase();
        }
    }

    public ProxyDefinitionBuilder protocol(ProxyProtocol proxyType) {
        this.proxyProtocol = proxyType;
        return this;
    }

    public ProxyDefinitionBuilder http() {
        return protocol(ProxyProtocol.http);
    }

    public ProxyDefinitionBuilder socks5() {
        return protocol(ProxyProtocol.socks5);
    }

    public static ProxyDefinitionBuilder through(String host, int port) {
        return through(FullSocketAddress.define(host, port));
    }

    public static ProxyDefinitionBuilder through(FullSocketAddress socketAddress) {
        return new ProxyDefinitionBuilder(socketAddress);
    }

    public ProxyDefinitionBuilder addProxyBypass(String bypassPattern) {
        requireNonNull(bypassPattern);
        Preconditions.checkArgument(!bypassPattern.trim().isEmpty(), "bypass pattern must be non-whitespace, non-empty string");
        proxyBypasses.add(bypassPattern);
        return this;
    }

    public ProxyDefinitionBuilder addProxyBypasses(List<String> bypassPatterns) {
        for (String p : bypassPatterns) {
            addProxyBypass(p);
        }
        return this;
    }

    private ProxySpecification buildUriSpec() {
        try {
            URIBuilder b = new URIBuilder();
            if (proxyProtocol != null) {
                b.setScheme(proxyProtocol.toScheme());
            }
            b.setHost(socketAddress.getHost())
                    .setPort(socketAddress.getPort());
            for (String bypass : proxyBypasses) {
                b.addParameter(UriProxySpecification.PARAM_BYPASS, bypass);
            }
            return UriProxySpecification.of(b.build());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public ProxySpecification build() {
        return buildUriSpec();
    }
}
