package io.github.mike10004.seleniumcapture;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Test;
import org.littleshoot.proxy.ChainedProxy;
import org.littleshoot.proxy.ChainedProxyAdapter;
import org.littleshoot.proxy.ChainedProxyType;
import org.littleshoot.proxy.impl.ClientDetails;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class UpstreamProxyManagerTest {

    @Test
    public void lookupChainedProxies_noBypass() {
        UpstreamProxyManager proxyManager = new UpstreamProxyManager(ChainedProxyType.HTTP, "127.0.0.1", 12345, null, null, UpstreamProxyManager.HostBypassPredicate.noBypass());
        testLookup(proxyManager, "example.com", Expectation.USE_CHAINED_PROXY);
    }

    @Test
    public void lookupChainedProxies_withBypassList() {
        List<HostBypassRule> bypassList = HostBypassRuleFactory.createDefault().fromSpecs(List.of("example.com", "localhost:59999"));
        UpstreamProxyManager proxyManager = new UpstreamProxyManager(ChainedProxyType.HTTP, "127.0.0.1", 12345, null, null, new ListHostBypassPredicate(bypassList));
        testLookup(proxyManager, "example.com", Expectation.BYPASS_AND_FALLBACK_TO_DIRECT);
        testLookup(proxyManager, "localhost:59999", Expectation.BYPASS_AND_FALLBACK_TO_DIRECT);
        testLookup(proxyManager, "localhost:54321", Expectation.USE_CHAINED_PROXY);
        testLookup(proxyManager, "localhost", Expectation.USE_CHAINED_PROXY);
        testLookup(proxyManager, "LOCALHOST", Expectation.USE_CHAINED_PROXY);
        testLookup(proxyManager, "www.example.com", Expectation.USE_CHAINED_PROXY);
        testLookup(proxyManager, "google.com", Expectation.USE_CHAINED_PROXY);
    }

    private enum Expectation {
        USE_CHAINED_PROXY,
        BYPASS_AND_FALLBACK_TO_DIRECT;

        public List<ChainedProxy> getExpectedList(UpstreamProxyManager proxyManager) {
            switch (this) {
                case USE_CHAINED_PROXY:
                    return List.of(proxyManager.getChainedProxy());
                case BYPASS_AND_FALLBACK_TO_DIRECT:
                    return List.of(ChainedProxyAdapter.FALLBACK_TO_DIRECT_CONNECTION);
            }
            throw new IllegalStateException("unhandled enum");
        }
    }

    private void testLookup(UpstreamProxyManager proxyManager, String hostAndPort, Expectation expectation) {
        DefaultFullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "https://" + hostAndPort + "/");
        ArrayDeque<ChainedProxy> chainedProxies = new ArrayDeque<>();
        proxyManager.lookupChainedProxies(httpRequest, chainedProxies, new ClientDetails());
        List<ChainedProxy> postLookupProxies = new ArrayList<>(chainedProxies);
        assertEquals("num proxies added", 1, postLookupProxies.size());
        List<ChainedProxy> expectedList = expectation.getExpectedList(proxyManager);
        assertEquals("proxy queue after lookup", expectedList, postLookupProxies);
    }
}