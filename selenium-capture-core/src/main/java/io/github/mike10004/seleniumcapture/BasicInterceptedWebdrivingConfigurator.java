package io.github.mike10004.seleniumcapture;

import com.google.common.base.MoreObjects;
import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.mitm.CertificateAndKeySource;
import org.littleshoot.proxy.ChainedProxyManager;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

class BasicInterceptedWebdrivingConfigurator implements InterceptedWebdrivingConfigurator {

    private final UpstreamProxyDefinition upstreamProxyDefinition;

    public BasicInterceptedWebdrivingConfigurator(UpstreamProxyDefinition upstreamProxyDefinition) {
        this.upstreamProxyDefinition = requireNonNull(upstreamProxyDefinition, "upstreamProxyDefinition");
    }

    @Override
    public void configureUpstream(BrowserUpProxy bmp) {
        if (upstreamProxyDefinition == null) {
            bmp.setChainedProxy(null);
            if (bmp instanceof BrowserUpProxyServer) {
                ((BrowserUpProxyServer) bmp).setChainedProxyManager(null);
            }
        } else {
            ChainedProxyManager chainedProxyManager = upstreamProxyDefinition.createUpstreamProxy();
            ((BrowserUpProxyServer)bmp).setChainedProxyManager(chainedProxyManager);
        }
    }

    @Override
    public WebdrivingConfig createWebdrivingConfig(BrowserUpProxy bup, @Nullable CertificateAndKeySource certificateAndKeySource) {
        WebdrivingProxyDefinition proxy = ProxyDefinitionBuilder.through(BrowserUps.resolveSocketAddress(bup))
                .addProxyBypasses(upstreamProxyDefinition.getProxyBypassList())
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
