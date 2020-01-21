package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.littleshoot.proxy.ChainedProxyManager;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

class BasicBmpConfigurator implements BmpConfigurator {

    private final UpstreamProxyDefinition upstreamProxyDefinition;

    public BasicBmpConfigurator(UpstreamProxyDefinition upstreamProxyDefinition) {
        this.upstreamProxyDefinition = requireNonNull(upstreamProxyDefinition, "upstreamProxyDefinition");
    }

    @Override
    public void configureUpstream(BrowserMobProxy bmp) {
        if (upstreamProxyDefinition == null) {
            bmp.setChainedProxy(null);
            if (bmp instanceof BrowserMobProxyServer) {
                ((BrowserMobProxyServer) bmp).setChainedProxyManager(null);
            }
        } else {
            ChainedProxyManager chainedProxyManager = upstreamProxyDefinition.createUpstreamProxy();
            ((BrowserMobProxyServer)bmp).setChainedProxyManager(chainedProxyManager);
        }
    }

    @Override
    public WebdrivingConfig createWebdrivingConfig(BrowserMobProxy bmp, @Nullable CertificateAndKeySource certificateAndKeySource) {
        WebdrivingProxyDefinition proxy = ProxyDefinitionBuilder.through(BrowserMobs.resolveSocketAddress(bmp))
                .buildWebdrivingProxyDefinition();
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
