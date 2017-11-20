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
    private final Supplier<BrowserMobProxy> localProxyInstantiator;
    private final ImmutableList<HarPostProcessor> harPostProcessors;

    /**
     * Constructs an instance of the class. Should only be used by subclasses that know
     * what they're doing. Otherwise, use {@link #builder(WebDriverFactory)} to create
     * an instance.
     * @param webDriverFactory web driver factory to use
     * @param certificateAndKeySource credential source
     * @param upstreamProxyProvider upstream proxy provider; can supply null if no upstream proxy is to be used
     * @param httpFiltersSources list of filters sources; this should probably include {@link AnonymizingFiltersSource}
     * @param localProxyInstantiator supplier that constructs the local proxy instance
     * @param harPostProcessors list of HAR post-processors
     */
    protected TrafficCollector(WebDriverFactory webDriverFactory,
                            @Nullable CertificateAndKeySource certificateAndKeySource,
                            Supplier<InetSocketAddress> upstreamProxyProvider,
                               Iterable<? extends HttpFiltersSource> httpFiltersSources,
                               Supplier<BrowserMobProxy> localProxyInstantiator,
                               Iterable<? extends HarPostProcessor> harPostProcessors) {
        this.webDriverFactory = checkNotNull(webDriverFactory);
        this.certificateAndKeySource = certificateAndKeySource;
        this.httpFiltersSources = ImmutableList.copyOf(httpFiltersSources);
        this.upstreamProxyProvider = checkNotNull(upstreamProxyProvider);
        this.localProxyInstantiator = checkNotNull(localProxyInstantiator);
        this.harPostProcessors = ImmutableList.copyOf(harPostProcessors);
    }

    private TrafficCollector(Builder builder) {
        this(builder.webDriverFactory,
                builder.certificateAndKeySource,
                builder.upstreamProxyProvider,
                builder.httpFiltersSources,
                builder.localProxyInstantiator,
                builder.harPostProcessors);
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
        BrowserMobProxy bmp = instantiateProxy();
        configureProxy(bmp, certificateAndKeySource, monitor);
        bmp.enableHarCaptureTypes(getCaptureTypes());
        bmp.newHar();
        bmp.start();
        R result;
        try {
            result = invokeGenerate(bmp, generator);
        } finally {
            bmp.stop();
        }
        Har har = bmp.getHar();
        for (HarPostProcessor harPostProcessor : harPostProcessors) {
            harPostProcessor.process(har);
        }
        return new HarPlus<>(har, result);
    }

    /**
     * Monitors traffic generated by the given generator. This like
     * {@link #collect(TrafficGenerator, TrafficMonitor)} but without capturing a HAR.
     * @param generator the traffic generator
     * @param monitor the monitor
     * @param <R> generator result type
     * @return the generator result
     * @throws IOException
     * @throws WebDriverException
     */
    public <R> R monitor(TrafficGenerator<R> generator, TrafficMonitor monitor) throws IOException, WebDriverException {
        checkNotNull(monitor, "monitor");
        return maybeMonitor(generator, monitor);
    }

    /**
     * Causes traffic to be generated by the given generator and returns the result.
     * This is essentially {@link #monitor(TrafficGenerator, TrafficMonitor)} without
     * notifying a monitor of request/response interactions.
     * @param generator the traffic generator
     * @param <R>
     * @return
     * @throws IOException
     * @throws WebDriverException
     */
    public <R> R drive(TrafficGenerator<R> generator) throws IOException, WebDriverException {
        return maybeMonitor(generator, null);
    }

    private <R> R maybeMonitor(TrafficGenerator<R> generator, @Nullable TrafficMonitor monitor) throws IOException, WebDriverException {
        checkNotNull(generator, "generator");
        BrowserMobProxy bmp = instantiateProxy();
        configureProxy(bmp, certificateAndKeySource, monitor);
        bmp.start();
        try {
            return invokeGenerate(bmp, generator);
        } finally {
            bmp.stop();
        }
    }

    private <R> R invokeGenerate(BrowserMobProxy bmp, TrafficGenerator<R> generator) throws IOException, WebDriverException {
        WebDriverConfig config = WebDriverConfig.builder()
                .proxy(BrowserMobs.getConnectableSocketAddress(bmp))
                .certificateAndKeySource(certificateAndKeySource)
                .build();
        WebDriver driver = webDriverFactory.createWebDriver(config);
        try {
            return generator.generate(driver);
        } finally {
            driver.quit();
        }
    }

    private class MonitorFiltersSource extends HttpFiltersSourceAdapter {

        private final TrafficMonitor monitor;

        private MonitorFiltersSource(TrafficMonitor monitor) {
            this.monitor = checkNotNull(monitor);
        }

        @Override
        public HttpFilters filterRequest(HttpRequest originalRequest) {
            return doFilterRequest(originalRequest, null);
        }

        @Override
        public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
            return doFilterRequest(originalRequest, ctx);
        }

        private HttpFilters doFilterRequest(HttpRequest originalRequest, @Nullable ChannelHandlerContext ctx) {
            if (!ProxyUtils.isCONNECT(originalRequest)) {
                return new TrafficMonitorFilter(originalRequest, ctx, monitor);
            } else {
                return null;
            }
        }

        @Override
        public int getMaximumRequestBufferSizeInBytes() {
            return monitor.getMaximumRequestBufferSizeInBytes();
        }

        @Override
        public int getMaximumResponseBufferSizeInBytes() {
            return monitor.getMaximumResponseBufferSizeInBytes();
        }
    }

    protected MitmManager createMitmManager(@SuppressWarnings("unused") BrowserMobProxy proxy, CertificateAndKeySource certificateAndKeySource) {
        MitmManager mitmManager = ImpersonatingMitmManager.builder()
                .rootCertificateSource(certificateAndKeySource)
                .build();
        return mitmManager;
    }

    protected BrowserMobProxy instantiateProxy() {
        return localProxyInstantiator.get();
    }

    protected void configureProxy(BrowserMobProxy bmp, CertificateAndKeySource certificateAndKeySource, @Nullable TrafficMonitor trafficMonitor) {
        if (certificateAndKeySource != null) {
            MitmManager mitmManager = createMitmManager(bmp, certificateAndKeySource);
            bmp.setMitmManager(mitmManager);
        }
        if (trafficMonitor != null) {
            bmp.addLastHttpFilterFactory(new MonitorFiltersSource(trafficMonitor));
        }
        httpFiltersSources.forEach(bmp::addLastHttpFilterFactory);
        @Nullable InetSocketAddress upstreamProxy = upstreamProxyProvider.get();
        if (upstreamProxy != null) {
            bmp.setChainedProxy(upstreamProxy);
        }
    }


    @SuppressWarnings("unused")
    public static final class Builder {

        private final WebDriverFactory webDriverFactory;
        private CertificateAndKeySource certificateAndKeySource = null;
        private final List<HttpFiltersSource> httpFiltersSources = new ArrayList<>();
        private Supplier<InetSocketAddress> upstreamProxyProvider = () -> null;
        private Supplier<BrowserMobProxy> localProxyInstantiator = BrowserMobProxyServer::new;
        private final List<HarPostProcessor> harPostProcessors = new ArrayList<>();

        private Builder(WebDriverFactory webDriverFactory) {
            this.webDriverFactory = checkNotNull(webDriverFactory);
            httpFiltersSources.add(AnonymizingFiltersSource.getInstance());
        }

        public Builder collectHttps(CertificateAndKeySource certificateAndKeySource) {
            this.certificateAndKeySource = checkNotNull(certificateAndKeySource);
            return this;
        }

        public Builder localProxyInstantiator(Supplier<BrowserMobProxy> localProxyInstantiator) {
            this.localProxyInstantiator = checkNotNull(localProxyInstantiator);
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

        public Builder harPostProcessor(HarPostProcessor harPostProcessor) {
            harPostProcessors.add(harPostProcessor);
            return this;
        }

        public TrafficCollector build() {
            return new TrafficCollector(this);
        }
    }
}
