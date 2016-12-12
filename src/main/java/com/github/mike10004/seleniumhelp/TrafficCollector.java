package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.filters.HttpsAwareFiltersAdapter;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import net.lightbody.bmp.proxy.CaptureType;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.MitmManager;
import org.littleshoot.proxy.impl.ProxyUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class TrafficCollector {

    private final WebDriverFactory webDriverFactory;
    private final CertificateAndKeySource certificateAndKeySource;
    private final ImmutableList<HttpFiltersSource> httpFiltersSources;
    private final java.util.function.Supplier<Optional<InetSocketAddress>> upstreamProxyProvider;

    public TrafficCollector(WebDriverFactory webDriverFactory, CertificateAndKeySource certificateAndKeySource,
                            HttpFiltersSource httpFiltersSource,
                            java.util.function.Supplier<Optional<InetSocketAddress>> upstreamProxyProvider) {
        this(webDriverFactory, certificateAndKeySource, ImmutableList.of(httpFiltersSource), upstreamProxyProvider);
    }

    public TrafficCollector(WebDriverFactory webDriverFactory, CertificateAndKeySource certificateAndKeySource,
                            Iterable<? extends HttpFiltersSource> httpFiltersSources,
                            java.util.function.Supplier<Optional<InetSocketAddress>> upstreamProxyProvider) {
        this.webDriverFactory = checkNotNull(webDriverFactory);
        this.certificateAndKeySource = checkNotNull(certificateAndKeySource);
        this.httpFiltersSources = ImmutableList.copyOf(httpFiltersSources);
        this.upstreamProxyProvider = checkNotNull(upstreamProxyProvider);
    }

    protected Set<CaptureType> getCaptureTypes() {
        return EnumSet.allOf(CaptureType.class);
    }

    protected static java.util.function.Supplier<Optional<InetSocketAddress>> absentUpstreamProxyProvider() {
        return Optional::empty;
    }

    /**
     * Collects traffic generated by the given generator into a HAR.
     * @param generator the generator
     * @return the HAR containing all traffic generated
     * @throws IOException if something I/O related goes awry
     * @throws WebDriverException if the web driver could not be created or the generator throws one
     */
    public <R> HarPlus<R> collect(TrafficGenerator<R> generator) throws IOException, WebDriverException {
        return collect(generator, null);
    }

    /**
     * Collects traffic generated by the given generator into a HAR.
     * @param generator the generator
     * @param listener the listener, or null
     * @return the HAR containing all traffic generated
     * @throws IOException if something I/O related goes awry
     * @throws WebDriverException if the web driver could not be created or the generator throws one
     */
    public <R> HarPlus<R> collect(TrafficGenerator<R> generator, @Nullable TrafficListener listener) throws IOException, WebDriverException {
        checkNotNull(generator, "generator");
        BrowserMobProxy bmp = createProxy(certificateAndKeySource);
        bmp.enableHarCaptureTypes(getCaptureTypes());
        bmp.newHar();
        if (listener != null) {
            addResponseNotificationFilters(bmp, listener);
        }
        bmp.start();
        R result;
        try {
            WebDriver driver = webDriverFactory.createWebDriver(bmp, certificateAndKeySource);
            try {
                result = generator.generate(driver);
            } finally {
                driver.quit();
            }
        } finally {
            bmp.stop();
        }
        Har har = bmp.getHar();
        return new HarPlus<>(har, result);
    }

    protected static class ResponseNotificationFilter extends HttpsAwareFiltersAdapter {

        private final TrafficListener trafficListener;

        public ResponseNotificationFilter(HttpRequest originalRequest, ChannelHandlerContext ctx, TrafficListener trafficListener) {
            super(originalRequest, ctx);
            this.trafficListener = checkNotNull(trafficListener);
        }

        @Override
        public HttpObject serverToProxyResponse(HttpObject httpObject) {
            if (httpObject instanceof HttpResponse) {
                trafficListener.responseReceived(new ImmutableHttpResponse((HttpResponse) httpObject));
            }
            return httpObject;
        }

        @Override
        public HttpResponse proxyToServerRequest(HttpObject httpObject) {
            if (httpObject instanceof HttpRequest) {
                trafficListener.sendingRequest(new ImmutableHttpRequest((HttpRequest) httpObject));
            }
            return null;
        }

    }

    protected void addResponseNotificationFilters(BrowserMobProxy bmp, final TrafficListener listener) {
        checkNotNull(listener, "listener");
        bmp.addLastHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                if (!ProxyUtils.isCONNECT(originalRequest)) {
                    return new ResponseNotificationFilter(originalRequest, ctx, listener);
                } else {
                    return null;
                }
            }
        });
    }

    protected MitmManager createMitmManager(BrowserMobProxy proxy, CertificateAndKeySource certificateAndKeySource) {
        MitmManager mitmManager = ImpersonatingMitmManager.builder()
                .rootCertificateSource(certificateAndKeySource)
                .build();
        return mitmManager;
    }

    protected BrowserMobProxy createProxy(CertificateAndKeySource certificateAndKeySource) {
        BrowserMobProxy bmp = new BrowserMobProxyServer();
        MitmManager mitmManager = createMitmManager(bmp, certificateAndKeySource);
        bmp.setMitmManager(mitmManager);
        httpFiltersSources.forEach(bmp::addLastHttpFilterFactory);
        InetSocketAddress upstreamProxy = upstreamProxyProvider.get().orElse(null);
        if (upstreamProxy != null) {
            bmp.setChainedProxy(upstreamProxy);
        }
        return bmp;
    }

}
