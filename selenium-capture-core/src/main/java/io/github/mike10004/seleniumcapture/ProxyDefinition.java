package io.github.mike10004.seleniumcapture;

import com.browserup.bup.BrowserUpProxy;

import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * Interface of a proxy definition.
 */
public interface ProxyDefinition extends UpstreamProxyDefinition, WebdrivingProxyDefinition {

    /**
     * Creates a proxy definition from a URI.
     * The protocol should
     * @param uri
     * @return
     */
    static ProxyDefinition fromUri(URI uri) {
        requireNonNull(uri, "uri");
        return UriProxyDefinition.of(uri);
    }

    static ProxyDefinition direct() {
        return UriProxyDefinition.noProxy();
    }

    static ProxyDefinitionBuilder builder(FullSocketAddress socketAddress) {
        return new ProxyDefinitionBuilder(socketAddress);
    }
}
