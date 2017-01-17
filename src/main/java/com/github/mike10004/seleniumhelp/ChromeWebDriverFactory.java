package com.github.mike10004.seleniumhelp;

import com.github.mike10004.chromecookieimplant.ChromeCookie;
import com.github.mike10004.xvfbselenium.WebDriverSupport;
import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import net.lightbody.bmp.BrowserMobProxy;
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
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public class ChromeWebDriverFactory extends EnvironmentWebDriverFactory {

    private final ChromeOptions chromeOptions;
    private final Capabilities capabilitiesOverrides;
    private final CookiePreparer cookiePreparer;

    public ChromeWebDriverFactory(Map<String, String> environment, ChromeOptions chromeOptions, Capabilities capabilitiesOverrides) {
        this(environment, chromeOptions, capabilitiesOverrides, cookielessPreparer);
    }

    public ChromeWebDriverFactory(Map<String, String> environment, ChromeOptions chromeOptions, Capabilities capabilitiesOverrides, CookiePreparer cookiePreparer) {
        super(environment);
        this.chromeOptions = checkNotNull(chromeOptions);
        this.capabilitiesOverrides = checkNotNull(capabilitiesOverrides);
        this.cookiePreparer = checkNotNull(cookiePreparer);
    }

    @SuppressWarnings("unused")
    public ChromeWebDriverFactory() {
        this(ImmutableMap.of(), new ChromeOptions(), new DesiredCapabilities());
    }

    public ChromeWebDriverFactory(Supplier<Map<String, String>> environmentSupplier,
                                  ChromeOptions chromeOptions, Capabilities capabilitiesOverrides) {
        this(environmentSupplier, chromeOptions, capabilitiesOverrides, cookielessPreparer);
    }

    public ChromeWebDriverFactory(Supplier<Map<String, String>> environmentSupplier,
                                  ChromeOptions chromeOptions, Capabilities capabilitiesOverrides, CookiePreparer cookiePreparer) {
        super(environmentSupplier);
        this.chromeOptions = checkNotNull(chromeOptions);
        this.capabilitiesOverrides = checkNotNull(capabilitiesOverrides);
        this.cookiePreparer = checkNotNull(cookiePreparer);
    }

    @Override
    public WebDriver createWebDriver(BrowserMobProxy proxy, CertificateAndKeySource certificateAndKeySource) throws IOException {
        cookiePreparer.supplementOptions(chromeOptions);
        DesiredCapabilities capabilities = toCapabilities(chromeOptions);
        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
        capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);
        capabilities.merge(capabilitiesOverrides);
        ChromeDriver driver = WebDriverSupport.chromeInEnvironment(environmentSupplier.get()).create(capabilities);
        cookiePreparer.prepareCookies(driver);
        return driver;
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

}

