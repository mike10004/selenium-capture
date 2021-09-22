package io.github.mike10004.seleniumcapture;

import com.browserup.bup.BrowserUpProxyServer;
import org.junit.Test;
import org.littleshoot.proxy.ChainedProxyManager;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BasicInterceptedWebdrivingConfiguratorTest {

    @Test
    public void createWebdrivingConfig_upstreamBypassNotSupported() {
        UriProxySpecification upstream =
                ProxyDefinitionBuilder.through("127.0.0.1", 39944)
                        .addProxyBypass("localhost")
                        .addProxyBypass("127.0.0.1")
                        .socks5().buildUriSpec();
        InterceptedWebdrivingConfigurator c = InterceptedWebdrivingConfigurator.usingUpstreamProxy(upstream.toUpstreamProxyDefinition());
        List<ChainedProxyManager> invokedSetChainedProxyManagers = new ArrayList<>();
        List<String> incorrectInvocations = new ArrayList<>();
        BrowserUpProxyServer bup = new BrowserUpProxyServer() {
            @Override
            public void setChainedProxy(InetSocketAddress chainedProxyAddress) {
                super.setChainedProxy(chainedProxyAddress);
                incorrectInvocations.add("setChainedProxy");
            }

            @Override
            public void setChainedProxyHTTPS(boolean chainedProxyHTTPS) {
                super.setChainedProxyHTTPS(chainedProxyHTTPS);
                incorrectInvocations.add("setChainedProxyHTTPS");
            }

            @Override
            public void setChainedProxyManager(ChainedProxyManager chainedProxyManager) {
                super.setChainedProxyManager(chainedProxyManager);
                invokedSetChainedProxyManagers.add(chainedProxyManager);
            }

            @Override
            public void setChainedProxyNonProxyHosts(List<String> upstreamNonProxyHosts) {
                super.setChainedProxyNonProxyHosts(upstreamNonProxyHosts);
                incorrectInvocations.add("setChainedProxyNonProxyHosts");
            }

            @Override
            public int getPort() {
                return 12345;
            }
        };

        WebdrivingConfig wdcfg = c.createWebdrivingConfig(bup, null);
        org.openqa.selenium.Proxy proxy = wdcfg.getProxySpecification().createWebdrivingProxy();
        assertEquals("proxy bypass spec", "localhost,127.0.0.1", proxy.getNoProxy());
        c.configureUpstream(bup);
        assertNull("deprecated ChainedProxy should not be used; instead, configurator should set a ChainedProxyManager", bup.getChainedProxy());
        assertEquals("setChainedProxyManager invoked once", 1, invokedSetChainedProxyManagers.size());
        assertNotNull("chainedProxyManager", invokedSetChainedProxyManagers.get(0));
        assertEquals("incorrect invocations", Collections.emptyList(), incorrectInvocations);
    }

}