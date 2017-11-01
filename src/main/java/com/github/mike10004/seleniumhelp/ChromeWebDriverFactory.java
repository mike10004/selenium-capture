package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbselenium.WebDriverSupport;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public class ChromeWebDriverFactory extends EnvironmentWebDriverFactory {

    private final ChromeOptions chromeOptions;
    private final Capabilities capabilitiesOverrides;
    private final CookiePreparer cookiePreparer;

    @SuppressWarnings("unused")
    public ChromeWebDriverFactory() {
        this(builder());
    }

    protected ChromeWebDriverFactory(Builder builder) {
        super(builder);
        chromeOptions = builder.chromeOptions;
        capabilitiesOverrides = builder.capabilitiesOverrides;
        cookiePreparer = builder.cookiePreparer;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public WebDriver createWebDriver(WebDriverConfig config) throws IOException {
        return createWebDriverMaybeWithProxy(config.getProxyAddress(), config.getCertificateAndKeySource());
    }

    private WebDriver createWebDriverMaybeWithProxy(@Nullable InetSocketAddress proxy, @Nullable CertificateAndKeySource certificateAndKeySource) throws IOException {
        cookiePreparer.supplementOptions(chromeOptions);
        DesiredCapabilities capabilities = toCapabilities(chromeOptions);
        if (proxy != null) {
            configureProxy(capabilities, proxy, certificateAndKeySource);
        }
        capabilities.merge(capabilitiesOverrides);
        ChromeDriver driver = WebDriverSupport.chromeInEnvironment(environmentSupplier.get()).create(capabilities);
        cookiePreparer.prepareCookies(driver);
        return driver;
    }

    /**
     * @deprecated use {@link #createWebDriver(WebDriverConfig)} with an empty config
     */
    @Deprecated
    public final WebDriver unproxied() throws IOException {
        return createWebDriverMaybeWithProxy(null, null);
    }

    /**
     * Configures a capabilities instance to use the given proxy. The certificate and key source
     * is not used because Chrome in webdriver mode appears to be fine with untrusted certificates,
     * which is good because we don't know how to install custom certificates.
     * @param capabilities the capabilities instance
     * @param proxy the proxy
     * @param certificateAndKeySource the certificate and key source (unused)
     */
    protected void configureProxy(DesiredCapabilities capabilities, InetSocketAddress proxy, @SuppressWarnings("unused") @Nullable CertificateAndKeySource certificateAndKeySource) {
        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
        capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);
    }

    protected DesiredCapabilities toCapabilities(ChromeOptions chromeOptions) {
        DesiredCapabilities capabilities = DesiredCapabilities.chrome();
        capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
        return capabilities;
    }

    /**
     * Interface for service classes that prepare cookies in a Chrome webdriver.
     */
    public interface CookiePreparer {
        /**
         * Parameterizes a given options instance with whatever is necessary to
         * prepare cookies in a Chrome browser instance. This is invoked prior to
         * instantiation of the webdriver.
         * @param options the options to parameterize
         * @throws IOException if I/O errors occur
         */
        void supplementOptions(ChromeOptions options) throws IOException;

        /**
         * Performs steps necessary to install cookies in a webdriver instance.
         * This is invoked after instantiation of the given driver but before
         * clients have access to it, that is, before it is returned by the
         * factory.
         * @param driver the webdriver
         * @throws WebDriverException should something go awry
         */
        void prepareCookies(ChromeDriver driver) throws WebDriverException;
    }

    private static final CookiePreparer cookielessPreparer = new CookiePreparer() {
        @Override
        public void supplementOptions(ChromeOptions options) {
            // no op
        }

        @Override
        public void prepareCookies(ChromeDriver driver) {
            // no op
        }
    };

    public static CookiePreparer makeCookieImplanter(Path scratchDir, Supplier<? extends Collection<DeserializableCookie>> cookiesSupplier) {
        return new ChromeCookiePreparer(scratchDir, cookiesSupplier);
    }

    @SuppressWarnings("unused")
    public static final class Builder extends EnvironmentWebDriverFactory.Builder<Builder> {

        private CookiePreparer cookiePreparer = cookielessPreparer;
        private Capabilities capabilitiesOverrides = new DesiredCapabilities();
        private ChromeOptions chromeOptions = new ChromeOptions();
        private boolean headless;

        private Builder() {
        }

        public Builder headless() {
            headless = true;
            return this;
        }

        public Builder chromeOptions(ChromeOptions val) {
            chromeOptions = checkNotNull(val);
            return this;
        }

        public Builder capabilitiesOverrides(Capabilities val) {
            capabilitiesOverrides = checkNotNull(val);
            return this;
        }

        public Builder cookiePreparer(CookiePreparer val) {
            cookiePreparer = checkNotNull(val);
            return this;
        }

        public ChromeWebDriverFactory build() {
            if (headless) {
                chromeOptions.addArguments("--headless", "--disable-gpu");
            }
            return new ChromeWebDriverFactory(this);
        }
    }

}

