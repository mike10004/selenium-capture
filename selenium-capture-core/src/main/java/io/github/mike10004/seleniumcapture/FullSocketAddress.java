package io.github.mike10004.seleniumcapture;

import com.google.common.net.HostAndPort;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;

import static java.util.Objects.requireNonNull;

public interface FullSocketAddress {

    String getHost();
    int getPort();

    static FullSocketAddress define(String host, int port) {
        return new WellDefinedSocketAddress(host, port);
    }

    static FullSocketAddress fromHostAndPort(HostAndPort hostAndPort) {
        return new WellDefinedSocketAddress(hostAndPort.getHost(), hostAndPort.getPort());
    }

    default URI toUri() {
        try {
            return new URIBuilder().setHost(getHost()).setPort(getPort()).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("probable host or port violation", e);
        }
    }
}

