package com.github.mike10004.seleniumhelp;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

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
    private final ProxyConfigurator upstreamConfigurator;
    private final Supplier<? extends BrowserMobProxy> interceptingProxyInstantiator;
    private final ImmutableList<HarPostProcessor> harPostProcessors;
    private final ExceptionReactor exceptionReactor;

    /**
     * Constructs an instance of the class. Should only be used by subclasses that know
     * what they're doing. Otherwise, use {@link TrafficCollector#builder(WebDriverFactory)} to create
     * an instance.
     * @param webDriverFactory web driver factory to use
     * @param certificateAndKeySource credential source
     * @param upstreamConfigurator upstream proxy configurator
     * @param httpFiltersSources list of filters sources; this should probably include {@link AnonymizingFiltersSource}
     * @param interceptingProxyInstantiator supplier that constructs the local proxy instance
     * @param harPostProcessors list of HAR post-processors
     * @param exceptionReactor exception reactor
     */
    protected TrafficCollectorImpl(WebDriverFactory webDriverFactory,
                            @Nullable CertificateAndKeySource certificateAndKeySource,
                            ProxyConfigurator upstreamConfigurator,
                               Iterable<? extends HttpFiltersSource> httpFiltersSources,
                               Supplier<? extends BrowserMobProxy> interceptingProxyInstantiator,
                               Iterable<? extends HarPostProcessor> harPostProcessors,
                               ExceptionReactor exceptionReactor) {
        this.webDriverFactory = requireNonNull(webDriverFactory);
        this.certificateAndKeySource = certificateAndKeySource;
        this.httpFiltersSources = ImmutableList.copyOf(httpFiltersSources);
        this.upstreamConfigurator = requireNonNull(upstreamConfigurator);
        this.interceptingProxyInstantiator = requireNonNull(interceptingProxyInstantiator);
        this.harPostProcessors = ImmutableList.copyOf(harPostProcessors);
        this.exceptionReactor = requireNonNull(exceptionReactor);
    }

    protected interface ProxyConfigurator {

        void configure(BrowserMobProxy proxy);

        static ProxyConfigurator inoperative() {
            return proxy -> {};
        }

        static ProxyConfigurator upstream(Supplier<? extends InetSocketAddress> upstreamProxyProvider) {
            return bmp -> {
                @Nullable InetSocketAddress upstreamProxy = upstreamProxyProvider.get();
                bmp.setChainedProxy(upstreamProxy);
            };
        }
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
        requireNonNull(generator, "generator");
        BrowserMobProxy bmp = instantiateProxy();
        configureProxy(bmp, certificateAndKeySource, monitor);
        bmp.enableHarCaptureTypes(getCaptureTypes());
        bmp.newHar();
        bmp.start();
        R result = null;
        try {
            result = invokeGenerate(bmp, generator, monitor);
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
        requireNonNull(monitor, "monitor");
        return maybeMonitor(generator, monitor);
    }

    @Override
    public <R> R drive(TrafficGenerator<R> generator) throws IOException, WebDriverException {
        return maybeMonitor(generator, null);
    }

    private <R> R maybeMonitor(TrafficGenerator<R> generator, @Nullable TrafficMonitor monitor) throws IOException, WebDriverException {
        requireNonNull(generator, "generator");
        BrowserMobProxy bmp = instantiateProxy();
        configureProxy(bmp, certificateAndKeySource, monitor);
        bmp.start();
        try {
            return invokeGenerate(bmp, generator, monitor);
        } catch (IOException | RuntimeException e){
            exceptionReactor.reactTo(e);
            return null;
        } finally {
            bmp.stop();
        }
    }

    private <R> R invokeGenerate(BrowserMobProxy bmp, TrafficGenerator<R> generator, @Nullable TrafficMonitor monitor) throws IOException, WebDriverException {
        WebDriverConfig config = WebDriverConfig.builder()
                .proxy(BrowserMobs.getConnectableSocketAddress(bmp))
                .certificateAndKeySource(certificateAndKeySource)
                .build();
        try (WebdrivingSession session = webDriverFactory.createWebdrivingSession(config)) {
            WebDriver webdriver = session.getWebDriver();
            if (monitor != null) {
                monitor.sessionCreated(new WeakReference<>(session));
            }
            return generator.generate(webdriver);
        }
    }

    private class MonitorFiltersSource extends HttpFiltersSourceAdapter {

        private final TrafficMonitor monitor;

        private MonitorFiltersSource(TrafficMonitor monitor) {
            this.monitor = requireNonNull(monitor);
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
        upstreamConfigurator.configure(bmp);
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper h = MoreObjects.toStringHelper(this);
        h.add("webDriverFactory", webDriverFactory);
        if (certificateAndKeySource != null) h.add("certificateAndKeySource", certificateAndKeySource);
        if (httpFiltersSources != null) h.add("httpFiltersSources", httpFiltersSources);
        if (upstreamConfigurator != null) h.add("upstreamConfigurator", upstreamConfigurator);
        if (interceptingProxyInstantiator != null) {
            h.add("interceptingProxyInstantiator", interceptingProxyInstantiator);
        }
        if (harPostProcessors != null) h.add("harPostProcessors.size", harPostProcessors.size());
        if (exceptionReactor != null) h.add("exceptionReactor", exceptionReactor);
        return h.toString();
    }
}
