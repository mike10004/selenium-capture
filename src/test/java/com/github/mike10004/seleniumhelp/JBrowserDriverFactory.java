package com.github.mike10004.seleniumhelp;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.ProxyConfig;
import com.machinepublishers.jbrowserdriver.Settings;
import org.openqa.selenium.WebDriver;

import javax.annotation.Nullable;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

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
        return new SimpleWebdrivingSession(createWebDriver(config));
    }

    /**
     * Creates a webdriver. The certificate and key source is ignored and the pem file passed to the constructor
     * of this instance is used instead.
     * @param config the config
     * @return
     */
    private WebDriver createWebDriver(WebDriverConfig config) {
        @Nullable InetSocketAddress proxy = config.getProxyAddress();
        Settings.Builder settingsBuilder = Settings.builder();
        if (proxy != null) {
            Set<String> nonProxyHosts = new HashSet<>(config.getProxyBypasses());
            ProxyConfig proxyConfig = new ProxyConfig(ProxyConfig.Type.HTTP, "localhost", proxy.getPort(), null, null, false, nonProxyHosts);
            settingsBuilder.proxy(proxyConfig);
        }
        if (certificatePemFile != null) {
            settingsBuilder.ssl(certificatePemFile.getAbsolutePath());
        }
        Settings settings = settingsBuilder.build();
        return new JBrowserDriver(settings);

    }
}
