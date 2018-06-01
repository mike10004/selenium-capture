package com.github.mike10004.seleniumhelp;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import net.lightbody.bmp.proxy.CaptureType;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.MitmManager;
import org.littleshoot.proxy.impl.ProxyUtils;
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
import static java.util.Objects.requireNonNull;

/**
 * Implementation of a traffic collector. Warning: this may become package-private or be renamed in a future release.
 */
@Beta
public class TrafficCollectorImpl implements TrafficCollector {

    private final WebDriverFactory webDriverFactory;
    @Nullable
    private final CertificateAndKeySource certificateAndKeySource;
    private final ImmutableList<HttpFiltersSource> httpFiltersSources;
    private final Supplier<InetSocketAddress> upstreamProxyProvider;
    private final Supplier<? extends BrowserMobProxy> interceptingProxyInstantiator;
    private final ImmutableList<HarPostProcessor> harPostProcessors;
    private final ExceptionReactor exceptionReactor;

    /**
     * Constructs an instance of the class. Should only be used by subclasses that know
     * what they're doing. Otherwise, use {@link TrafficCollector#builder(WebDriverFactory)} to create
     * an instance.
     * @param webDriverFactory web driver factory to use
     * @param certificateAndKeySource credential source
     * @param upstreamProxyProvider upstream proxy provider; can supply null if no upstream proxy is to be used
     * @param httpFiltersSources list of filters sources; this should probably include {@link AnonymizingFiltersSource}
     * @param interceptingProxyInstantiator supplier that constructs the local proxy instance
     * @param harPostProcessors list of HAR post-processors
     * @param exceptionReactor exception reactor
     */
    protected TrafficCollectorImpl(WebDriverFactory webDriverFactory,
                            @Nullable CertificateAndKeySource certificateAndKeySource,
                            Supplier<InetSocketAddress> upstreamProxyProvider,
                               Iterable<? extends HttpFiltersSource> httpFiltersSources,
                               Supplier<? extends BrowserMobProxy> interceptingProxyInstantiator,
                               Iterable<? extends HarPostProcessor> harPostProcessors,
                               ExceptionReactor exceptionReactor) {
        this.webDriverFactory = checkNotNull(webDriverFactory);
        this.certificateAndKeySource = certificateAndKeySource;
        this.httpFiltersSources = ImmutableList.copyOf(httpFiltersSources);
        this.upstreamProxyProvider = checkNotNull(upstreamProxyProvider);
        this.interceptingProxyInstantiator = checkNotNull(interceptingProxyInstantiator);
        this.harPostProcessors = ImmutableList.copyOf(harPostProcessors);
        this.exceptionReactor = requireNonNull(exceptionReactor);
    }

    /**
     * Constructs an instance of the class from the given builder.
     * @param builder the builder
     */
    protected TrafficCollectorImpl(Builder builder) {
        this(builder.webDriverFactory,
                builder.certificateAndKeySource,
                builder.upstreamProxyProvider,
                builder.httpFiltersSources,
                builder.interceptingProxyInstantiator,
                builder.harPostProcessors,
                builder.exceptionReactor);
    }

    protected Set<CaptureType> getCaptureTypes() {
        return EnumSet.allOf(CaptureType.class);
    }

    @Override
    public <R> HarPlus<R> collect(TrafficGenerator<R> generator) throws IOException, WebDriverException {
        return collect(generator, null);
    }

    @Override
    public <R> HarPlus<R> collect(TrafficGenerator<R> generator, @Nullable TrafficMonitor monitor) throws IOException, WebDriverException {
        checkNotNull(generator, "generator");
        BrowserMobProxy bmp = instantiateProxy();
        configureProxy(bmp, certificateAndKeySource, monitor);
        bmp.enableHarCaptureTypes(getCaptureTypes());
        bmp.newHar();
        bmp.start();
        R result = null;
        try {
            result = invokeGenerate(bmp, generator);
        } catch (IOException | RuntimeException e) {
            exceptionReactor.reactTo(e);
        } finally {
            bmp.stop();
        }
        Har har = bmp.getHar();
        for (HarPostProcessor harPostProcessor : harPostProcessors) {
            harPostProcessor.process(har);
        }
        return new HarPlus<>(har, result);
    }

    @Override
    public <R> R monitor(TrafficGenerator<R> generator, TrafficMonitor monitor) throws IOException, WebDriverException {
        checkNotNull(monitor, "monitor");
        return maybeMonitor(generator, monitor);
    }

    @Override
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
        } catch (IOException | RuntimeException e){
            exceptionReactor.reactTo(e);
            return null;
        } finally {
            bmp.stop();
        }
    }

    private <R> R invokeGenerate(BrowserMobProxy bmp, TrafficGenerator<R> generator) throws IOException, WebDriverException {
        WebDriverConfig config = WebDriverConfig.builder()
                .proxy(BrowserMobs.getConnectableSocketAddress(bmp))
                .certificateAndKeySource(certificateAndKeySource)
                .build();
        try (WebdrivingSession session = webDriverFactory.createWebdrivingSession(config)) {
            return generator.generate(session.getWebDriver());
        }
    }

    @SuppressWarnings("unused")
    public static final class Builder {

        private final WebDriverFactory webDriverFactory;
        private CertificateAndKeySource certificateAndKeySource = null;
        private final List<HttpFiltersSource> httpFiltersSources = new ArrayList<>();
        private Supplier<InetSocketAddress> upstreamProxyProvider = () -> null;
        private Supplier<? extends BrowserMobProxy> interceptingProxyInstantiator = BrAwareBrowserMobProxyServer::new;
        private final List<HarPostProcessor> harPostProcessors = new ArrayList<>();
        private ExceptionReactor exceptionReactor = ExceptionReactor.PROPAGATE;

        Builder(WebDriverFactory webDriverFactory) {
            this.webDriverFactory = checkNotNull(webDriverFactory);
            httpFiltersSources.add(AnonymizingFiltersSource.getInstance());
        }

        public Builder onException(ExceptionReactor exceptionReactor) {
            this.exceptionReactor = requireNonNull(exceptionReactor);
            return this;
        }

        public Builder collectHttps(CertificateAndKeySource certificateAndKeySource) {
            this.certificateAndKeySource = checkNotNull(certificateAndKeySource);
            return this;
        }

        /**
         * Sets the supplier of the proxy server instance that is used to intercept and collect traffic.
         * By default, we supply a custom implementation that supports brotli decoding,
         * {@link BrAwareBrowserMobProxyServer}. To revert this behavior to a more hands-off implementation,
         * set this to a supplier of a {@link net.lightbody.bmp.BrowserMobProxyServer} instance.
         * @param interceptingProxyInstantiator the instantiator
         * @return this builder instance
         */
        public Builder interceptingProxyInstantiator(Supplier<? extends BrowserMobProxy> interceptingProxyInstantiator) {
            this.interceptingProxyInstantiator = checkNotNull(interceptingProxyInstantiator);
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

        /**
         * Adds all argument filters sources to this builder's filters list.
         * @param val the filters sources to add
         * @return this instance
         */
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
            return new TrafficCollectorImpl(this);
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
        return interceptingProxyInstantiator.get();
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
}
