package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbselenium.WebDriverSupport;
import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.apache.http.client.utils.URIBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

    public interface CookiePreparer {
        void supplementOptions(ChromeOptions options) throws IOException;
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

    public static class CookieImplantOutput {

        public static class CookieImplantResult {
            public int index;
            public boolean success;
            public String message;
            public ChromeCookie savedCookie;
        }

        public enum CookieImplantStatus {
            not_yet_processed, some_imports_processed, all_imports_processed
        }

        public CookieImplantStatus status;
        public List<CookieImplantResult> imports;
    }

    public static CookiePreparer makeCookieImplanter(Path scratchDir, Supplier<? extends Collection<DeserializableCookie>> cookiesSupplier) {
        return new CookieImplanter(scratchDir, cookiesSupplier);
    }

    static class CookieImplanter implements CookiePreparer {

        private static final Logger log = LoggerFactory.getLogger(CookieImplanter.class);

        private static final String COOKIE_IMPLANT_EXTENSION_ID = "njacaggbgbhpimllodfhihjndngkadjh";
        private static final String COOKIE_IMPLANT_EXTENSION_VERSION = "1.4";
        private static final String COOKIE_IMPLANT_EXTENSION_RESOURCE_PATH = "/chrome-cookie-implant/" + COOKIE_IMPLANT_EXTENSION_ID + "-" + COOKIE_IMPLANT_EXTENSION_VERSION + ".crx";

        private final URL cookieImplantCrxResource;
        private final Path scratchDir;
        private final Supplier<? extends Collection<DeserializableCookie>> cookiesSupplier;
        private final transient Gson gson = buildChromeCookieGson();
        private final transient ChromeCookieTransform chromeCookieTransform = new ChromeCookieTransform();
        private final int outputTimeoutSeconds = 10;

        public CookieImplanter(Path scratchDir, Supplier<? extends Collection<DeserializableCookie>> cookiesSupplier) {
            this(scratchDir, cookiesSupplier, CookieImplanter.class.getResource(COOKIE_IMPLANT_EXTENSION_RESOURCE_PATH));
        }

        public CookieImplanter(Path scratchDir, Supplier<? extends Collection<DeserializableCookie>> cookiesSupplier, URL cookieImplantCrxResource) {
            this.cookieImplantCrxResource = checkNotNull(cookieImplantCrxResource, "cookie implant .crx resource");
            this.scratchDir = checkNotNull(scratchDir);
            this.cookiesSupplier = checkNotNull(cookiesSupplier);
        }

        protected Gson buildChromeCookieGson() {
            return new GsonBuilder()
                    .addSerializationExclusionStrategy(new ExclusionStrategy() {
                        @Override
                        public boolean shouldSkipField(FieldAttributes f) {
                            return false;
                        }

                        @Override
                        public boolean shouldSkipClass(Class<?> clazz) {
                            return false;
                        }
                    }).create();
        }

        @Override
        public void supplementOptions(ChromeOptions options) throws IOException {
            File crxFile;
            if ("file".equals(cookieImplantCrxResource.getProtocol())) {
                try {
                    crxFile = new File(cookieImplantCrxResource.toURI());
                } catch (URISyntaxException e) {
                    throw new FileNotFoundException("not a valid uri: " + cookieImplantCrxResource);
                }
            } else {
                crxFile = File.createTempFile("chrome-cookie-implant", ".crx", scratchDir.toFile());
                ByteSource cookieImplantCrxSource = Resources.asByteSource(cookieImplantCrxResource);
                cookieImplantCrxSource.copyTo(com.google.common.io.Files.asByteSink(crxFile));
            }
            options.addExtensions(crxFile);
        }

        @Override
        public void prepareCookies(ChromeDriver driver) throws WebDriverException {
            Collection<DeserializableCookie> inputCookies = cookiesSupplier.get();
            URI manageUrl = buildImplantUriFromCookies(inputCookies.stream());
            driver.get(manageUrl.toString());
            CookieImplantOutput output = waitForCookieImplantOutput(driver, outputTimeoutSeconds);
            int numFailures = 0;
            for (CookieImplantOutput.CookieImplantResult result : output.imports) {
                if (!result.success) {
                    log.warn("cookie implant failed: {} {}", result.index, result.message);
                    numFailures++;
                }
            }
            if (numFailures > 0) {
                throw new CookieImplantException(numFailures + " cookies failed to be implanted");
            }
            log.debug("{} cookies imported using implant extension", inputCookies.size());
            driver.get("data:,"); // blank page
        }

        @SuppressWarnings("unused")
        protected static class CookieImplantException extends WebDriverException {
            public CookieImplantException() {
            }

            public CookieImplantException(String message) {
                super(message);
            }

            public CookieImplantException(Throwable cause) {
                super(cause);
            }

            public CookieImplantException(String message, Throwable cause) {
                super(message, cause);
            }
        }

        protected <T> By elementTextRepresentsObject(By elementLocator, Class<T> deserializedType, Predicate<? super T> predicate) {
            return Bys.elementWithText(elementLocator, json -> {
                if (json == null) {
                    return false;
                }
                T thing = gson.fromJson(json, deserializedType);
                return predicate.test(thing);
            });
        }

        protected By byOutputStatus(Predicate<CookieImplantOutput.CookieImplantStatus> statusPredicate) {
            return elementTextRepresentsObject(By.cssSelector("#output"), CookieImplantOutput.class, cio -> statusPredicate.test(cio.status));
        }

        protected CookieImplantOutput waitForCookieImplantOutput(WebDriver driver, int timeOutInSeconds) {
            WebElement outputElement = new WebDriverWait(driver, timeOutInSeconds)
                    .until(ExpectedConditions.presenceOfElementLocated(byOutputStatus(CookieImplantOutput.CookieImplantStatus.all_imports_processed::equals)));
            String outputJson = outputElement.getText();
            CookieImplantOutput output = gson.fromJson(outputJson, CookieImplantOutput.class);
            return output;
        }


        protected URI buildImplantUriFromCookies(Stream<DeserializableCookie> cookies) {
            return buildImplantUriFromCookieJsons(cookies
                    .map(chromeCookieTransform::transform)
                    .map(gson::toJson));
        }

        protected URI buildImplantUriFromCookieJsons(Stream<String> cookieJsons) {
            try {
                URIBuilder uriBuilder = new URIBuilder(URI.create("chrome-extension://" + COOKIE_IMPLANT_EXTENSION_ID + "/manage.html"));
                cookieJsons.forEach(cookieJson -> uriBuilder.addParameter("import", cookieJson));
                URI uri = uriBuilder.build();
                return uri;
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }

        }
    }

    public static class ChromeCookieTransform {

        public ChromeCookie transform(DeserializableCookie input) {
            ChromeCookie output = new ChromeCookie();
            output.url = fabricateUrlFromDomain(input.getBestDomainProperty(), input.isHttpOnly(), input.getPath());
            output.name = input.getName();
            output.value = input.getValue();
            output.domain = input.getBestDomainProperty();
            output.path = input.getPath();
            Date expiryDate = input.getExpiryDate();
            output.expirationDate = expiryDate == null ? null : expiryDate.getTime() / 1000d;
            output.secure = input.isSecure();
            output.httpOnly = input.isHttpOnly();
            output.sameSite = ChromeCookie.SameSiteStatus.no_restriction;
            return output;
        }

        private static CharMatcher dotMatcher = CharMatcher.is('.');
        private static CharMatcher slashMatcher = CharMatcher.is('/');

        protected String fabricateUrlFromDomain(String domain, boolean secure, String path) {
            if (Strings.isNullOrEmpty(domain)) {
                LoggerFactory.getLogger(ChromeCookieTransform.class).warn("input cookie has no domain, so no URL can be fabricated; chrome will not like this");
                return "";
            }
            domain = dotMatcher.trimLeadingFrom(domain);
            String scheme = secure ? "https" : "http";
            path = slashMatcher.trimLeadingFrom(Strings.nullToEmpty(path));
            return scheme + "://" + domain + "/" + path;
        }

    }
}
