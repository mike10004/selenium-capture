package io.github.mike10004.seleniumcapture;

import com.google.common.base.Preconditions;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Service class that provides methods to build a proxy definition.
 * @see ProxyDefinition
 */
public class ProxyDefinitionBuilder {

    private final FullSocketAddress socketAddress;
    private final List<String> proxyBypasses;

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
    public ProxyDefinition http() {
        return build(ProxyProtocol.http);
    }

    public ProxyDefinition socks5() {
        return build(ProxyProtocol.socks5);
    }

    public static ProxyDefinitionBuilder through(String host, int port) {
        return through(FullSocketAddress.define(host, port));
    }

    public static ProxyDefinitionBuilder through(FullSocketAddress socketAddress) {
        return new ProxyDefinitionBuilder(socketAddress);
    }

    /**
     * Adds a proxy bypass rule.
     * See {@link SeleniumProxies#makeNoProxyValue(List)} for an
     * admittedly deficient description of syntax.
     *
     * @param bypassPattern bypass rule
     * @return this builder instance
     */
    public ProxyDefinitionBuilder addProxyBypass(String bypassPattern) {
        requireNonNull(bypassPattern);
        Preconditions.checkArgument(!bypassPattern.trim().isEmpty(), "bypass pattern must be non-whitespace, non-empty string");
        proxyBypasses.add(bypassPattern);
        return this;
    }

    /**
     * Invokes {@link #addProxyBypass(String)} for each rule in the given list.
     * @return this builder instance
     */
    public ProxyDefinitionBuilder addProxyBypasses(List<String> bypassPatterns) {
        for (String p : bypassPatterns) {
            addProxyBypass(p);
        }
        return this;
    }

    private ProxyDefinition buildUriSpec(ProxyProtocol proxyProtocol) {
        requireNonNull(proxyProtocol, "proxyProtocol");
        try {
            URIBuilder b = new URIBuilder();
            b.setScheme(proxyProtocol.toScheme());
            b.setHost(socketAddress.getHost())
                    .setPort(socketAddress.getPort());
            if (!proxyBypasses.isEmpty()) {
                // just for looks; if there are query params, it looks weird without path
                b.setPath("/");
            }
            for (String bypass : proxyBypasses) {
                b.addParameter(UriProxyDefinition.PARAM_BYPASS, bypass);
            }
            return UriProxyDefinition.of(b.build());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public ProxyDefinition build(ProxyProtocol proxyProtocol) {
        return buildUriSpec(proxyProtocol);
    }
}
