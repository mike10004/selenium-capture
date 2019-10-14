package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;

import static java.util.Objects.requireNonNull;

public interface FullSocketAddress {

    String getHost();
    int getPort();

    static FullSocketAddress define(String host, int port) {
        return new WellDefinedSocketAddress(host, port);
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
        MoreObjects.ToStringHelper h = MoreObjects.toStringHelper(this);
        if (host != null) h.add("host", host);
        h.add("port", port);
        return h.toString();
    }
}