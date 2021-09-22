package io.github.mike10004.seleniumcapture;

import java.net.URI;

import static java.util.Objects.requireNonNull;

public interface ProxySpecification {

    static ProxySpecification fromUri(URI uri) {
        requireNonNull(uri, "uri");
        return UriProxySpecification.of(uri);
    }

    UpstreamProxyDefinition asUpstream();

    WebdrivingProxyDefinition asWebdriving();

}
