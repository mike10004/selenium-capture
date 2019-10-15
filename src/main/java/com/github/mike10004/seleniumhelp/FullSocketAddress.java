package com.github.mike10004.seleniumhelp;

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

final class WellDefinedSocketAddress implements FullSocketAddress {

    private final String host;
    private final int port;

    public WellDefinedSocketAddress(String host, int port) {
        requireNonNull(host, "host");
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("0 < port <= 65535 is required");
        }
        if (host.isEmpty()) {
            throw new IllegalArgumentException("host must be nonempty string");
        }
        this.host = host;
        this.port = port;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return String.format("%s:%s", getHost(), getPort());
    }

}