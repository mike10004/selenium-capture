package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbselenium.WebDriverSupport;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class FirefoxWebDriverFactory extends OptionalDisplayWebDriverFactory {

    public FirefoxWebDriverFactory() {
    }

    public FirefoxWebDriverFactory(String display) {
        super(display);
    }

    @Override
    public WebDriver createWebDriver(BrowserMobProxy proxy, CertificateAndKeySource certificateAndKeySource) throws IOException {
        FirefoxProfile profile;
        if (certificateAndKeySource instanceof FirefoxCompatibleCertificateSource) {
            ByteSource certificateDbByteSource = ((FirefoxCompatibleCertificateSource)certificateAndKeySource).getFirefoxCertificateDatabase();
            profile = new CertificateSupplementingFirefoxProfile(certificateDbByteSource);
        } else {
            profile = new FirefoxProfile();
        }
        // https://stackoverflow.com/questions/2887978/webdriver-and-proxy-server-for-firefox
        profile.setPreference("network.proxy.type", 1);
        profile.setPreference("network.proxy.http", "localhost");
        profile.setPreference("network.proxy.http_port", proxy.getPort());
        profile.setPreference("network.proxy.ssl", "localhost");
        profile.setPreference("network.proxy.ssl_port", proxy.getPort());
        profile.setPreference("network.proxy.no_proxies_on", ""); // no host bypassing; collector should get all traffic
        profile.setPreference("browser.search.geoip.url", "");
        profile.setPreference("network.prefetch-next", false);
        profile.setPreference("network.http.speculative-parallel-limit", 0);
        profile.setPreference("browser.aboutHomeSnippets.updateUrl", "");
        profile.setPreference("extensions.getAddons.cache.enabled", false);
        profile.setPreference("media.gmp-gmpopenh264.enabled", false);
        profile.setPreference("browser.newtabpage.enabled", false);
        profile.setPreference("app.update.url", "");
        profile.setPreference("browser.safebrowsing.provider.mozilla.updateURL", "");
        FirefoxBinary binary = createFirefoxBinary();
        Map<String, String> environment = buildEnvironment();
        FirefoxDriver driver = WebDriverSupport.firefoxInEnvironment(environment).create(binary, profile);
        return driver;
    }

    protected FirefoxBinary createFirefoxBinary() {
        return new FirefoxBinary();
    }

    private static class CertificateSupplementingFirefoxProfile extends SupplementingFirefoxProfile {

        public static final String CERTIFICATE_DB_FILENAME = "cert8.db";

        private final ByteSource certificateDbSource;

        private CertificateSupplementingFirefoxProfile(ByteSource certificateDbSource) {
            this.certificateDbSource = checkNotNull(certificateDbSource);
        }

        @Override
        protected void profileCreated(File profileDir) {
            File certificateDbFile = new File(profileDir, CERTIFICATE_DB_FILENAME);
            try {
                certificateDbSource.copyTo(Files.asByteSink(certificateDbFile));
            } catch (IOException e) {
                throw new IllegalStateException("failed to copy certificate database to profile dir", e);
            }
        }
    }

    private static abstract class SupplementingFirefoxProfile extends FirefoxProfile {

        protected abstract void profileCreated(File profileDir);

        @Override
        public File layoutOnDisk() {
            File profileDir = super.layoutOnDisk();
            profileCreated(profileDir);
            return profileDir;
        }
    }
}
