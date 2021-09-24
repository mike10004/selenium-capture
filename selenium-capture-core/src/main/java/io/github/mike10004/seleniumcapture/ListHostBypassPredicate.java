package io.github.mike10004.seleniumcapture;

import com.google.common.base.Objects;
import com.google.common.net.HostAndPort;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

class ListHostBypassPredicate implements UpstreamProxyManager.HostBypassPredicate {

    private final List<HostBypassRule> bypassedHosts;

    public ListHostBypassPredicate(List<HostBypassRule> bypassedHosts) {
        this.bypassedHosts = List.copyOf(bypassedHosts);
    }

    @Override
    public boolean isBypass(String httpRequestUri) {
        try {
            URI uri = new URI(httpRequestUri);
            for (HostBypassRule bypassRule : bypassedHosts) {
                if (bypassRule.isBypass(uri)) {
                    return true;
                }
            }
        } catch (URISyntaxException ignore) {
        } catch (RuntimeException e) {
            LoggerFactory.getLogger(getClass()).info("host bypass rule exception", e);
        }
        return false;
    }

}
