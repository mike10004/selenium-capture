/*
 * (c) 2017 Novetta
 *
 * Created by mike
 */
package com.github.mike10004.seleniumhelp;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.ProxyConfig;
import com.machinepublishers.jbrowserdriver.Settings;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.openqa.selenium.WebDriver;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

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
    public WebDriver createWebDriver(BrowserMobProxy proxy, @Nullable CertificateAndKeySource certificateAndKeySource) throws IOException {
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
