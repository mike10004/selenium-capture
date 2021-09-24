package io.github.mike10004.seleniumcapture;

import com.browserup.bup.BrowserUpProxy;

public interface UpstreamProxyDefinition {

    void configureUpstreamProxy(BrowserUpProxy browserUpProxy, HostBypassRuleFactory bypassRuleFactory);

    static UpstreamProxyDefinition direct() {
        return NoProxyDefinition.noUpstreamProxy();
    }
}
