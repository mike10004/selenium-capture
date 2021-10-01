package io.github.mike10004.seleniumcapture;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.net.HostAndPort;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.net.URI;
import java.net.URISyntaxException;

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
            String hostSpecHost = hostSpec.getHost();
            // HostAndPort always strips [] from IPv6 host
            // URI always adds/retains [] from IPv6 host
            if (InetAddressValidator.getInstance().isValidInet6Address(hostSpecHost)) {
                hostSpecHost = "[" + hostSpecHost + "]";
            }
            boolean hostsEqual = Objects.equal(uriHost, hostSpecHost);
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("hostSpec", hostSpec)
                .toString();
    }
}
