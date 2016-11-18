package com.github.mike10004.seleniumhelp;

import com.google.common.annotations.VisibleForTesting;
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

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;


import static com.google.common.base.Preconditions.checkNotNull;

public class TrafficCollector {

    private final WebDriverFactory webDriverFactory;
    private final CertificateAndKeySource certificateAndKeySource;
    private final HttpFiltersSource httpFiltersSource;

    public TrafficCollector(WebDriverFactory webDriverFactory, CertificateAndKeySource certificateAndKeySource, HttpFiltersSource httpFiltersSource) {
        this.webDriverFactory = checkNotNull(webDriverFactory);
        this.certificateAndKeySource = checkNotNull(certificateAndKeySource);
        this.httpFiltersSource = checkNotNull(httpFiltersSource);
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

    protected @Nullable InetSocketAddress detectJvmProxy() {
        return detectJvmProxy(System.getProperties());
    }

    /**
     * Detects JVM proxy settings.
     * @param sysprops the system properties
     * @return the socket address of the proxy
     * @throws IllegalStateException if the HTTP and HTTPS proxy settings are inconsistent
     * @throws NumberFormatException if the proxy ports are not integers
     */
    @VisibleForTesting
    protected @Nullable InetSocketAddress detectJvmProxy(Properties sysprops) throws IllegalStateException, NumberFormatException {
        String httpProxyHost = sysprops.getProperty("http.proxyHost");
        String httpProxyPort = sysprops.getProperty("http.proxyPort");
        String httpsProxyHost = sysprops.getProperty("http.proxyHost");
        String httpsProxyPort = sysprops.getProperty("http.proxyPort");
        if (!Objects.equals(httpProxyHost, httpsProxyHost)) {
            throw new IllegalStateException("system properties define conflicting values for http.proxyHost=" + httpProxyHost + " and httpsProxyHost=" + httpsProxyHost);
        }
        if (!Objects.equals(httpProxyPort, httpsProxyPort)) {
            throw new IllegalStateException("system properties define conflicting values for http.proxyPort=" + httpProxyPort + " and httpsProxyPort=" + httpsProxyPort);
        }
        if ((httpsProxyHost == null) != (httpsProxyPort == null)) {
            throw new IllegalStateException("nullness of https.proxyHost=" + httpsProxyHost + " and https.proxyPort=" + httpsProxyPort + " system properties must be consistent");
        }
        if (httpsProxyHost != null) {
            return new InetSocketAddress(httpsProxyHost, Integer.parseInt(httpsProxyPort));
        } else {
            return null;
        }
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
        InetSocketAddress upstreamProxyAddress = detectJvmProxy();
        if (upstreamProxyAddress != null) {
            bmp.setChainedProxy(upstreamProxyAddress);
        }
        return bmp;
    }

}
