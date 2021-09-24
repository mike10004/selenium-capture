package io.github.mike10004.seleniumcapture;

import com.google.common.base.Objects;
import com.google.common.net.HostAndPort;

import java.net.URI;

import static java.util.Objects.requireNonNull;

class HostnameLiteralBypassRule implements HostBypassRule {

    private final HostAndPort hostSpec;

    public HostnameLiteralBypassRule(HostAndPort hostSpec) {
        this.hostSpec = requireNonNull(hostSpec);
    }

    private static int selectDefaultPort(URI uri) {
        String scheme = uri.getScheme();
        if (scheme != null && scheme.equalsIgnoreCase("https")) {
            return 443;
        }
        return 80;
    }

    @Override
    public boolean isBypass(URI uri) {
        String uriHost = uri.getHost();
        if (uriHost != null) {
            uriHost = uriHost.toLowerCase();
            boolean hostsEqual = Objects.equal(uriHost, hostSpec.getHost());
            if (hostsEqual) {
                if (hostSpec.hasPort()) {
                    int bypassPort = hostSpec.getPort();
                    int uriPort = uri.getPort();
                    if (uriPort < 0) {
                        uriPort = selectDefaultPort(uri);
                    }
                    return bypassPort == uriPort;
                } else {
                    return true;
                }
            }
        }
        return false;
    }
}
