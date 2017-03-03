package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.core.har.Har;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public class TrafficCollector {

    private final WebDriverFactory webDriverFactory;
    @Nullable
    private final CertificateAndKeySource certificateAndKeySource;
    private final ImmutableList<HttpFiltersSource> httpFiltersSources;
    private final Supplier<InetSocketAddress> upstreamProxyProvider;

    /**
     * Constructs an instance of the class. Should only be used by subclasses that know
     * what they're doing. Otherwise, use
     * @param webDriverFactory web driver factory to use
     * @param certificateAndKeySource credential source
     * @param upstreamProxyProvider upstream proxy provider; can supply null if no upstream proxy is to be used
     * @param httpFiltersSources list of filters sources; this should probably include {@link AnonymizingFiltersSource}
     */
    protected TrafficCollector(WebDriverFactory webDriverFactory,
                            @Nullable CertificateAndKeySource certificateAndKeySource,
                            Supplier<InetSocketAddress> upstreamProxyProvider,
                               Iterable<? extends HttpFiltersSource> httpFiltersSources) {
        this.webDriverFactory = checkNotNull(webDriverFactory);
        this.certificateAndKeySource = certificateAndKeySource;
        this.httpFiltersSources = ImmutableList.copyOf(httpFiltersSources);
        this.upstreamProxyProvider = checkNotNull(upstreamProxyProvider);
    }

    private TrafficCollector(Builder builder) {
        webDriverFactory = builder.webDriverFactory;
        certificateAndKeySource = builder.certificateAndKeySource;
        httpFiltersSources = ImmutableList.copyOf(builder.httpFiltersSources);
        upstreamProxyProvider = builder.upstreamProxyProvider;
    }

    public static Builder builder(WebDriverFactory webDriverFactory) {
        return new Builder(webDriverFactory);
    }

    protected Set<CaptureType> getCaptureTypes() {
        return EnumSet.allOf(CaptureType.class);
    }

    /**
     * Collects traffic generated by the given generator into a HAR. This invokes
     * {@link #collect(TrafficGenerator, TrafficMonitor)} with a null monitor reference.
     * @param generator the generator
     * @param <R> type of result the generator returns
     * @return the HAR containing all traffic generated
     * @throws IOException if something I/O related goes awry
     * @throws WebDriverException if the web driver could not be created or the generator throws one
     */
    public <R> HarPlus<R> collect(TrafficGenerator<R> generator) throws IOException, WebDriverException {
        return collect(generator, null);
    }

    /**
     * Collects traffic generated by the given generator into a HAR. Notifications of request/response
     * interactions can be sent to the given monitor, optionally.
     * @param generator the generator
     * @param <R> type of result the generator returns
     * @param monitor a monitor, or null
     * @return the HAR containing all traffic generated
     * @throws IOException if something I/O related goes awry
     * @throws WebDriverException if the web driver could not be created or the generator throws one
     */
    public <R> HarPlus<R> collect(TrafficGenerator<R> generator, @Nullable TrafficMonitor monitor) throws IOException, WebDriverException {
        checkNotNull(generator, "generator");
        BrowserMobProxy bmp = createProxy(certificateAndKeySource);
        bmp.enableHarCaptureTypes(getCaptureTypes());
        bmp.newHar();
        if (monitor != null) {
            addTrafficMonitorFilter(bmp, monitor);
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

    protected void addTrafficMonitorFilter(BrowserMobProxy bmp, final TrafficMonitor monitor) {
        checkNotNull(monitor, "monitor");
        bmp.addLastHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                if (!ProxyUtils.isCONNECT(originalRequest)) {
                    return new TrafficMonitorFilter(originalRequest, ctx, monitor);
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
        if (certificateAndKeySource != null) {
            MitmManager mitmManager = createMitmManager(bmp, certificateAndKeySource);
            bmp.setMitmManager(mitmManager);
        }
        httpFiltersSources.forEach(bmp::addLastHttpFilterFactory);
        @Nullable InetSocketAddress upstreamProxy = upstreamProxyProvider.get();
        if (upstreamProxy != null) {
            bmp.setChainedProxy(upstreamProxy);
        }
        return bmp;
    }


    public static final class Builder {

        private final WebDriverFactory webDriverFactory;
        private CertificateAndKeySource certificateAndKeySource = null;
        private final List<HttpFiltersSource> httpFiltersSources = new ArrayList<>();
        private Supplier<InetSocketAddress> upstreamProxyProvider = () -> null;

        private Builder(WebDriverFactory webDriverFactory) {
            this.webDriverFactory = checkNotNull(webDriverFactory);
            httpFiltersSources.add(AnonymizingFiltersSource.getInstance());
        }

        public Builder collectHttps(CertificateAndKeySource certificateAndKeySource) {
            this.certificateAndKeySource = checkNotNull(certificateAndKeySource);
            return this;
        }

        public Builder nonAnonymizing() {
            httpFiltersSources.remove(AnonymizingFiltersSource.getInstance());
            return this;
        }

        public Builder filter(HttpFiltersSource filter) {
            httpFiltersSources.add(filter);
            return this;
        }

        public Builder filters(Collection<? extends HttpFiltersSource> val) {
            httpFiltersSources.addAll(val);
            return this;
        }

        public Builder upstreamProxy(InetSocketAddress address) {
            upstreamProxyProvider = () -> address;
            return this;
        }

        public Builder upstreamProxy(Supplier<InetSocketAddress> val) {
            upstreamProxyProvider = checkNotNull(val);
            return this;
        }

        public TrafficCollector build() {
            return new TrafficCollector(this);
        }
    }
}
