package io.github.mike10004.seleniumcapture;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.mitm.CertificateAndKeySource;
import com.google.common.base.MoreObjects;

import javax.annotation.Nullable;

import java.util.List;

import static java.util.Objects.requireNonNull;

class BasicInterceptedWebdrivingConfigurator implements InterceptedWebdrivingConfigurator {

    private final UpstreamProxyDefinition upstreamProxyDefinition;
    private final HostBypassRuleFactory upstreamHostBypassRuleFactory;
    private final List<String> webdrivingProxyBypassList;

    public BasicInterceptedWebdrivingConfigurator(UpstreamProxyDefinition upstreamProxyDefinition, HostBypassRuleFactory upstreamHostBypassRuleFactory, List<String> webdrivingProxyBypassList) {
        this.upstreamProxyDefinition = requireNonNull(upstreamProxyDefinition, "upstreamProxyDefinition");
        this.webdrivingProxyBypassList = List.copyOf(webdrivingProxyBypassList);
        this.upstreamHostBypassRuleFactory = requireNonNull(upstreamHostBypassRuleFactory);
    }

    @Override
    public void configureUpstream(BrowserUpProxy bmp) {
        upstreamProxyDefinition.configureUpstreamProxy(bmp, upstreamHostBypassRuleFactory);
    }

    @Override
    public WebdrivingConfig createWebdrivingConfig(BrowserUpProxy bup,
                                                   @Nullable CertificateAndKeySource certificateAndKeySource) {
        WebdrivingProxyDefinition proxy = ProxyDefinitionBuilder.through(BrowserUps.resolveSocketAddress(bup))
                .addProxyBypasses(webdrivingProxyBypassList)
                .http();
        return WebdrivingConfig.builder()
                .proxy(proxy)
                .certificateAndKeySource(certificateAndKeySource)
                .build();
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper h = MoreObjects.toStringHelper(this);
        h.add("upstreamProxyDefinition", upstreamProxyDefinition);
        return h.toString();
    }

}
