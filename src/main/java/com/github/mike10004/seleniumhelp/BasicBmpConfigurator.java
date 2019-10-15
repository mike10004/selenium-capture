package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.littleshoot.proxy.ChainedProxyManager;

import javax.annotation.Nullable;
import java.net.URI;

class BasicBmpConfigurator implements BmpConfigurator {

    @Nullable
    private final URI upstreamProxyUri;

    public BasicBmpConfigurator(@Nullable URI upstreamProxyUri) {
        this.upstreamProxyUri = upstreamProxyUri;
    }

    @Override
    public void configureUpstream(BrowserMobProxy bmp) {
        if (upstreamProxyUri == null) {
            bmp.setChainedProxy(null);
            if (bmp instanceof BrowserMobProxyServer) {
                ((BrowserMobProxyServer) bmp).setChainedProxyManager(null);
            }
        } else {
            ProxySpecification proxySpecification = UriProxySpecification.of(upstreamProxyUri);
            ChainedProxyManager chainedProxyManager = proxySpecification.toUpstreamProxy();
            ((BrowserMobProxyServer)bmp).setChainedProxyManager(chainedProxyManager);
        }
    }

    @Override
    public WebdrivingConfig createWebdrivingConfig(BrowserMobProxy bmp, @Nullable CertificateAndKeySource certificateAndKeySource) {
        ProxySpecification proxy = ProxySpecification.throughSocketAddress(BrowserMobs.resolveSocketAddress(bmp));
        return WebdrivingConfigs.builder()
                .proxy(proxy)
                .certificateAndKeySource(certificateAndKeySource)
                .build();
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper h = MoreObjects.toStringHelper(this);
        if (upstreamProxyUri != null) h.add("upstreamProxyUri", upstreamProxyUri);
        return h.toString();
    }
}
