package io.github.mike10004.seleniumcapture;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import org.junit.Test;
import org.littleshoot.proxy.ChainedProxyManager;
import org.openqa.selenium.Proxy;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BasicInterceptedWebdrivingConfiguratorTest {

    @Test
    public void configurator() {
        UpstreamProxyDefinition upstreamProxy =
                ProxyDefinitionBuilder.through("1.1.1.1", 3128)
                        .addProxyBypass("localhost")
                        .addProxyBypass("127.0.0.1")
                        .socks5();
        InterceptedWebdrivingConfigurator interceptedWebdrivingConfigurator = new BasicInterceptedWebdrivingConfigurator(upstreamProxy, HostBypassRuleFactory.createDefault(), Collections.emptyList());
        List<ChainedProxyManager> invokedSetChainedProxyManagers = new ArrayList<>();
        List<String> incorrectInvocations = new ArrayList<>();
        BrowserUpProxy bup = new BrowserUpProxyServer() {
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

        WebdrivingConfig webdrivingConfig = interceptedWebdrivingConfigurator.createWebdrivingConfig(bup, null);
        org.openqa.selenium.Proxy proxy = webdrivingConfig.getProxySpecification().createWebdrivingProxy();
        assertEquals("proxyType", Proxy.ProxyType.MANUAL, proxy.getProxyType());
        assertEquals("proxy address", "127.0.1.1:12345", proxy.getHttpProxy());
        assertNull("bypass", proxy.getNoProxy());
        interceptedWebdrivingConfigurator.configureUpstream(bup);
        assertNull("deprecated ChainedProxy should not be used; instead, configurator should set a ChainedProxyManager", bup.getChainedProxy());
        assertEquals("setChainedProxyManager invoked once", 1, invokedSetChainedProxyManagers.size());
        assertNotNull("chainedProxyManager", invokedSetChainedProxyManagers.get(0));
        assertEquals("incorrect invocations", Collections.emptyList(), incorrectInvocations);
    }

}