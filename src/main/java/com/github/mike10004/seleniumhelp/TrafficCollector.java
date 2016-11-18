package com.github.mike10004.seleniumhelp;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import net.lightbody.bmp.proxy.CaptureType;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.MitmManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class TrafficCollector {

    private final WebDriverFactory webDriverFactory;
    private final CertificateAndKeySource certificateAndKeySource;
    private final HttpFiltersSource httpFiltersSource;
    private final Supplier<Optional<InetSocketAddress>> upstreamProxyProvider;

    public TrafficCollector(WebDriverFactory webDriverFactory, CertificateAndKeySource certificateAndKeySource, HttpFiltersSource httpFiltersSource, Supplier<Optional<InetSocketAddress>> upstreamProxyProvider) {
        this.webDriverFactory = checkNotNull(webDriverFactory);
        this.certificateAndKeySource = checkNotNull(certificateAndKeySource);
        this.httpFiltersSource = checkNotNull(httpFiltersSource);
        this.upstreamProxyProvider = checkNotNull(upstreamProxyProvider);
    }

    protected Set<CaptureType> getCaptureTypes() {
        return EnumSet.allOf(CaptureType.class);
    }

    public Har collect(TrafficGenerator generator) throws IOException, WebDriverException {
        BrowserMobProxy bmp = createProxy(certificateAndKeySource);
        Set<CaptureType> captureTypes = getCaptureTypes();

        bmp.enableHarCaptureTypes(captureTypes);
        bmp.newHar();
        bmp.start();
        try {
            WebDriver driver = webDriverFactory.createWebDriver(bmp, certificateAndKeySource);
            try {
                generator.generate(driver);
            } finally {
                driver.quit();
            }
        } finally {
            bmp.stop();
        }
        net.lightbody.bmp.core.har.Har har = bmp.getHar();
        return har;

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
        bmp.addLastHttpFilterFactory(httpFiltersSource);
        InetSocketAddress upstreamProxy = upstreamProxyProvider.get().orNull();
        if (upstreamProxy != null) {
            bmp.setChainedProxy(upstreamProxy);
        }
        return bmp;
    }

}
