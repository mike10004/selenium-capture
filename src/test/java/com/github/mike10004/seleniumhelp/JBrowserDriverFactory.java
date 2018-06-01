package com.github.mike10004.seleniumhelp;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.ProxyConfig;
import com.machinepublishers.jbrowserdriver.Settings;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.openqa.selenium.WebDriver;

import javax.annotation.Nullable;
import java.io.File;
import java.net.InetSocketAddress;

import static java.util.Objects.requireNonNull;

public class JBrowserDriverFactory implements WebDriverFactory {

    @Nullable
    private final File certificatePemFile;

    public JBrowserDriverFactory() {
        this(null);
    }

    public JBrowserDriverFactory(@Nullable File certificatePemFile) {
        this.certificatePemFile = certificatePemFile;
    }

    @Override
    public WebdrivingSession createWebdrivingSession(WebDriverConfig config) {
        return WebdrivingSession.simple(createWebDriver(config.getProxyAddress(), config.getCertificateAndKeySource()));
    }

    private WebDriver createWebDriver(InetSocketAddress proxy,
              @SuppressWarnings("unused") @Nullable CertificateAndKeySource certificateAndKeySource) {
        requireNonNull(proxy, "proxy");
        ProxyConfig proxyConfig = new ProxyConfig(ProxyConfig.Type.HTTP, "localhost", proxy.getPort());
        Settings.Builder settingsBuilder = Settings.builder()
                .proxy(proxyConfig);
        if (certificatePemFile != null) {
            settingsBuilder.ssl(certificatePemFile.getAbsolutePath());
        }
        Settings settings = settingsBuilder.build();
        return new JBrowserDriver(settings);

    }
}
