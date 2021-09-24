package io.github.mike10004.seleniumcapture;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import org.openqa.selenium.Proxy;

import static java.util.Objects.requireNonNull;

class NoProxyDefinition implements ProxyDefinition {

    private final UpstreamProxyDefinition upstreamDelegate;
    private final WebdrivingProxyDefinition webdrivingDelegate;

    public NoProxyDefinition() {
        this(noUpstreamProxy(), noWebdrivingProxy());
    }

    private NoProxyDefinition(UpstreamProxyDefinition upstreamDelegate, WebdrivingProxyDefinition webdrivingDelegate) {
        this.upstreamDelegate = requireNonNull(upstreamDelegate);
        this.webdrivingDelegate = requireNonNull(webdrivingDelegate);
    }

    @Override
    public void configureUpstreamProxy(BrowserUpProxy browserUpProxy, HostBypassRuleFactory bypassRuleFactory) {
        upstreamDelegate.configureUpstreamProxy(browserUpProxy, bypassRuleFactory);
    }

    static UpstreamProxyDefinition noUpstreamProxy() {
        return new NoUpstreamProxy();
    }

    static WebdrivingProxyDefinition noWebdrivingProxy() {
        return new NoWebdrivingProxy();
    }

    private static class NoUpstreamProxy implements UpstreamProxyDefinition {
        @Override
        public void configureUpstreamProxy(BrowserUpProxy browserUpProxy,
                                           HostBypassRuleFactory bypassRuleFactory) {
            browserUpProxy.setChainedProxy(null);
            if (browserUpProxy instanceof BrowserUpProxyServer) {
                ((BrowserUpProxyServer) browserUpProxy).setChainedProxyManager(null);
            }
        }
    }

    @Override
    public Proxy createWebdrivingProxy() {
        return webdrivingDelegate.createWebdrivingProxy();
    }

    static Proxy directSeleniumProxy() {
        return new Proxy().setProxyType(Proxy.ProxyType.DIRECT);
    }

    private static class NoWebdrivingProxy implements WebdrivingProxyDefinition {

        public NoWebdrivingProxy() {}

        @Override
        public Proxy createWebdrivingProxy() {
            return directSeleniumProxy();
        }
    }
}
