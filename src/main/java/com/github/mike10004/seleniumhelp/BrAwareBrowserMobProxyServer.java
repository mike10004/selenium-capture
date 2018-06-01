package com.github.mike10004.seleniumhelp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.filters.HttpConnectHarCaptureFilter;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.impl.ProxyUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class BrAwareBrowserMobProxyServer extends BrowserMobProxyServer  {

    /**
     * Set to true once the HAR capture filter has been added to the filter chain.
     */
    private final AtomicBoolean harCaptureFilterEnabled = new AtomicBoolean(false);

    public BrAwareBrowserMobProxyServer() {
    }

    /**
     * Enables the HAR capture filter if it has not already been enabled. The filter will be added to the end of the filter chain.
     * The HAR capture filter is relatively expensive, so this method is only called when a HAR is requested.
     */
    @Override
    protected void addHarCaptureFilter() {
        if (harCaptureFilterEnabled.compareAndSet(false, true)) {
            // the HAR capture filter is (relatively) expensive, so only enable it when a HAR is being captured. furthermore,
            // restricting the HAR capture filter to requests where the HAR exists, as well as  excluding HTTP CONNECTs
            // from the HAR capture filter, greatly simplifies the filter code.
            addHttpFilterFactory(new HttpFiltersSourceAdapter() {
                @Override
                public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                    Har har = getHar();
                    if (har != null && !ProxyUtils.isCONNECT(originalRequest)) {
                        return new BrAwareHarCaptureFilter(originalRequest, ctx, har, getCurrentHarPage() == null ? null : getCurrentHarPage().getId(), getHarCaptureTypes());
                    } else {
                        return null;
                    }
                }
            });

            // HTTP CONNECTs are a special case, since they require special timing and error handling
            addHttpFilterFactory(new HttpFiltersSourceAdapter() {
                @Override
                public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                    Har har = getHar();
                    if (har != null && ProxyUtils.isCONNECT(originalRequest)) {
                        return new HttpConnectHarCaptureFilter(originalRequest, ctx, har, getCurrentHarPage() == null ? null : getCurrentHarPage().getId());
                    } else {
                        return null;
                    }
                }
            });
        }
    }
}
