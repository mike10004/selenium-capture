package io.github.mike10004.seleniumcapture;

import com.google.common.base.Objects;
import com.google.common.net.HostAndPort;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

class ListHostBypassPredicate implements UpstreamProxy.HostBypassPredicate {

    private final List<HostAndPort> bypassedHosts;

    public ListHostBypassPredicate(List<String> bypassedHosts) {
        this.bypassedHosts = bypassedHosts.stream()
                .map(String::toLowerCase)
                .map(HostAndPort::fromString)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isBypass(String httpRequestUri) {
        try {
            URI uri = new URI(httpRequestUri);
            String uriHost = uri.getHost();
            if (uriHost != null) {
                uriHost = uriHost.toLowerCase();
                for (HostAndPort bypassCandidate : bypassedHosts) {
                    boolean hostsEqual = Objects.equal(uriHost, bypassCandidate.getHost());
                    if (hostsEqual) {
                        if (bypassCandidate.hasPort()) {
                            int bypassPort = bypassCandidate.getPort();
                            int uriPort = uri.getPort();
                            if (uriPort < 0) {
                                uriPort = selectDefaultPort(uri);
                            }
                            if (bypassPort == uriPort) {
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                }
            }
        } catch (URISyntaxException ignore) {
        }
        return false;
    }

    private static int selectDefaultPort(URI uri) {
        String scheme = uri.getScheme();
        if (scheme != null && scheme.equalsIgnoreCase("https")) {
            return 443;
        }
        return 80;
    }
}
