package com.github.mike10004.seleniumhelp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ChromeWebDriverFactory extends EnvironmentWebDriverFactory {

    private static final Logger log = LoggerFactory.getLogger(ChromeWebDriverFactory.class);

    private final ChromeOptions chromeOptions;
    private final CookiePreparer cookiePreparer;
    private final ImmutableList<DriverServiceBuilderConfigurator> driverServiceBuilderConfigurators;

    @SuppressWarnings("unused")
    public ChromeWebDriverFactory() {
        this(builder());
    }

    protected ChromeWebDriverFactory(Builder builder) {
        super(builder);
        chromeOptions = builder.chromeOptions;
        driverServiceBuilderConfigurators = ImmutableList.copyOf(builder.driverServiceBuilderConfigurators);
        cookiePreparer = builder.cookiePreparer;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public WebdrivingSession startWebdriving(WebdrivingConfig config) throws IOException {
        ServicedSession session = createWebDriverMaybeWithProxy(config);
        return session;
    }

    private ServicedSession createWebDriverMaybeWithProxy(WebdrivingConfig config) throws IOException {
        configureProxy(chromeOptions, config);
        cookiePreparer.supplementOptions(chromeOptions);
        ChromeDriverService.Builder serviceBuilder = createDriverServiceBuilder();
        serviceBuilder.withEnvironment(environmentSupplier.get());
        driverServiceBuilderConfigurators.forEach(configurator -> configurator.configure(serviceBuilder));
        ChromeDriverService service = serviceBuilder.build();
        final ChromeDriver driver;
        try {
            driver = new ChromeDriver(service, chromeOptions);
        } catch (WebDriverException e) { // on failure to start
            if (service.isRunning()) {
                log.warn("failed to construct ChromeDriver, but driver service is still running; trying to stop");
                try {
                    service.stop();
                } catch (RuntimeException e2) {
                    log.error("failed to stop driver service", e2);
                }
            }
            throw e;
        }
        cookiePreparer.prepareCookies(driver);
        return new ServicedSession(driver, service);
    }

    protected ChromeDriverService.Builder createDriverServiceBuilder() {
        return new ChromeDriverService.Builder().usingAnyFreePort();
    }

    @SuppressWarnings("unchecked")
    static List<String> getArguments(ChromeOptions options) {
        Map<String, ?> json = options.toJson();
        Map<String, ?> googOptions = (Map<String, ?>) json.get("goog:chromeOptions");
        checkArgument(googOptions != null, "args not present in %s", json);
        return (List<String>) googOptions.get("args");
    }

    /**
     * Parameterizes a capabilities instance according to a config instance. The certificate and key source
     * is not used because Chrome in webdriver mode appears to be fine with untrusted certificates,
     * which is good because we don't know how to install custom certificates.
     * @param options the options instance
     * @param config the config instance
     */
    protected void configureProxy(ChromeOptions options, WebdrivingConfig config) {
        @Nullable ProxySpecification proxySpecification = config.getProxySpecification();
        @Nullable org.openqa.selenium.Proxy seleniumProxy = null;
        if (proxySpecification != null) {
            seleniumProxy = maybeModifyBypassList(proxySpecification.createSeleniumProxy());
        }
        options.setProxy(seleniumProxy);
    }

    /**
     * Adjusts the list of bypass patterns if necessary.
     *
     * Special attention is required when the bypass list does not include localhost (or any loopback address),
     * as noted here: https://bugs.chromium.org/p/chromium/issues/detail?id=899126#c18
     *
     * If no bypasses are specified -- meaning the proxy should <i>not</i> be bypassed even for localhost --
     * then we have to explicitly say so by actually want to specify is {@code <-loopback>}.
     * @return an adjusted proxy instance
     */
    @Nullable
    private org.openqa.selenium.Proxy maybeModifyBypassList(@Nullable org.openqa.selenium.Proxy proxy) {
        if (proxy == null) {
            return null;
        }
        if (Strings.isNullOrEmpty(proxy.getNoProxy())) {
            proxy.setNoProxy("<-loopback>");
        }
        return proxy;
    }

    @VisibleForTesting
    ChromeOptions getChromeOptions() {
        return chromeOptions;
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

    public interface DriverServiceBuilderConfigurator {
        void configure(ChromeDriverService.Builder builder);
    }

    @SuppressWarnings("unused")
    public static final class Builder extends EnvironmentWebDriverFactory.Builder<Builder> {

        private CookiePreparer cookiePreparer = cookielessPreparer;
        private ChromeOptions chromeOptions = new ChromeOptions();
        private List<DriverServiceBuilderConfigurator> driverServiceBuilderConfigurators = new ArrayList<>();
        private boolean headless;

        private Builder() {
        }

        public Builder driverServiceBuilderConfigurator(DriverServiceBuilderConfigurator configurator) {
            driverServiceBuilderConfigurators.add(configurator);
            return this;
        }

        public Builder headless() {
            headless = true;
            return this;
        }

        public Builder chromeOptions(ChromeOptions val) {
            chromeOptions = checkNotNull(val);
            return this;
        }

        public Builder cookiePreparer(CookiePreparer val) {
            cookiePreparer = checkNotNull(val);
            return this;
        }

        public ChromeWebDriverFactory build() {
            if (headless) {
                chromeOptions.addArguments(HEADLESS_ARGS);
            }
            return new ChromeWebDriverFactory(this);
        }

        @VisibleForTesting
        static final ImmutableList<String> HEADLESS_ARGS = ImmutableList.of("--headless", "--disable-gpu");
    }

}

