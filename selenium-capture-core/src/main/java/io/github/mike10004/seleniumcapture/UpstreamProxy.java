package io.github.mike10004.seleniumcapture;

import com.google.common.annotations.VisibleForTesting;
import io.netty.handler.codec.http.HttpRequest;
import org.littleshoot.proxy.ChainedProxy;
import org.littleshoot.proxy.ChainedProxyAdapter;
import org.littleshoot.proxy.ChainedProxyManager;
import org.littleshoot.proxy.ChainedProxyType;
import org.littleshoot.proxy.impl.ClientDetails;

import java.net.InetSocketAddress;
import java.util.Queue;

import static java.util.Objects.requireNonNull;

/**
 * @author https://gist.github.com/jbaldassari/a13f9032999e82711a282d0c7a4b452c
 */
class UpstreamProxy implements ChainedProxyManager {

    private final ChainedProxyType proxyType;
    private final String host;
    private final int port;
    private final HostBypassPredicate hostBypassPredicate;
    private final ChainedProxy chainedProxy;

    public UpstreamProxy(final ChainedProxyType proxyType, final String host, final int port,
                  final String username, final String password, HostBypassPredicate hostBypassPredicate) {
        this.proxyType = proxyType;
        this.host = host;
        this.port = port;
        this.hostBypassPredicate = requireNonNull(hostBypassPredicate);
        this.chainedProxy = new ChainedProxyAdapter() {
            @Override
            public ChainedProxyType getChainedProxyType() {
                return proxyType;
            }

            @Override
            public InetSocketAddress getChainedProxyAddress() {
                return new InetSocketAddress(host, port);
            }

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public String getPassword() {
                return password;
            }

            @Override
            public String toString() {
                return "ChainedProxy{" + proxyType.name().toLowerCase() + "://" + host + ":" + port + "}";
            }
        };
    }

    @VisibleForTesting
    ChainedProxy getChainedProxy() {
        return chainedProxy;
    }

    public interface HostBypassPredicate {

        static HostBypassPredicate noBypass() {
            return new HostBypassPredicate() {
                @Override
                public boolean isBypass(String httpRequestUri) {
                    return false;
                }

                @Override
                public String toString() {
                    return "HostBypassPredicate{NONE}";
                }
            };
        }

        boolean isBypass(String httpRequestUri);

    }

    @Override
    public void lookupChainedProxies(HttpRequest httpRequest, Queue<ChainedProxy> chainedProxies, ClientDetails clientDetails) {
        if (hostBypassPredicate.isBypass(httpRequest.uri())) {
            chainedProxies.add(ChainedProxyAdapter.FALLBACK_TO_DIRECT_CONNECTION);
        } else {
            chainedProxies.add(chainedProxy);
        }
    }

    @Override
    public String toString() {
        return "UpstreamChainedProxyManager{" + proxyType.name().toLowerCase() + "://" + host + ":" + port + "}";
    }
}
