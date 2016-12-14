package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbselenium.WebDriverSupport;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.apache.commons.lang3.StringUtils;
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

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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

    public ChromeWebDriverFactory(Supplier<Map<String, String>> environmentSupplier, ChromeOptions chromeOptions, Capabilities capabilitiesOverrides) {
        this(environmentSupplier, chromeOptions, capabilitiesOverrides, cookielessPreparer);
    }

    public ChromeWebDriverFactory(Supplier<Map<String, String>> environmentSupplier, ChromeOptions chromeOptions, Capabilities capabilitiesOverrides, CookiePreparer cookiePreparer) {
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
            public JsonObject savedCookie;
        }

        public String status;
        public List<CookieImplantResult> imports;
    }

    public static CookiePreparer makeCookieImplanter(Path scratchDir, Supplier<? extends Collection<DeserializableCookie>> cookiesSupplier) {
        return new CookieImplanter(scratchDir, cookiesSupplier);
    }

    static class CookieImplanter implements CookiePreparer {

        private static final Logger log = LoggerFactory.getLogger(CookieImplanter.class);

        private static final String COOKIE_IMPLANT_EXTENSION_ID = "neiaahbjfbepoclbammdhcailekhmcdm";
        private static final String COOKIE_IMPLANT_EXTENSION_VERSION = "1.1";
        private static final String COOKIE_IMPLANT_EXTENSION_RESOURCE_PATH = "/chrome-cookie-implant/" + COOKIE_IMPLANT_EXTENSION_ID + "-" + COOKIE_IMPLANT_EXTENSION_VERSION + ".crx";

        private final ByteSource cookieImplantCrxSource;
        private final Path scratchDir;
        private final Supplier<? extends Collection<DeserializableCookie>> cookiesSupplier;
        private final transient Gson gson = new Gson();
        private final int outputTimeoutSeconds = 3;

        public CookieImplanter(Path scratchDir, Supplier<? extends Collection<DeserializableCookie>> cookiesSupplier) {
            this(scratchDir, cookiesSupplier, Resources.asByteSource(CookieImplanter.class.getResource(COOKIE_IMPLANT_EXTENSION_RESOURCE_PATH)));
        }

        public CookieImplanter(Path scratchDir, Supplier<? extends Collection<DeserializableCookie>> cookiesSupplier, ByteSource cookieImplantCrxSource) {
            this.cookieImplantCrxSource = checkNotNull(cookieImplantCrxSource);
            this.scratchDir = checkNotNull(scratchDir);
            this.cookiesSupplier = checkNotNull(cookiesSupplier);
        }

        @Override
        public void supplementOptions(ChromeOptions options) throws IOException {
            File crxFile = File.createTempFile("chrome-cookie-implant", ".crx", scratchDir.toFile());
            cookieImplantCrxSource.copyTo(com.google.common.io.Files.asByteSink(crxFile));
            options.addExtensions(crxFile);
        }

        @Override
        public void prepareCookies(ChromeDriver driver) throws WebDriverException {
            Collection<DeserializableCookie> inputCookies = cookiesSupplier.get();
            URI manageUrl = buildImplantUriFromCookies(inputCookies.stream());
            driver.get(manageUrl.toString());
            JsonObject outputJson = waitForCookieImplantOutput(driver, outputTimeoutSeconds);
            if (!outputJson.has("imports") || !outputJson.get("imports").isJsonArray()) {
                throw new CookieImplantException("output.imports has unexpected type: " + outputJson.get("imports"));
            }
            CookieImplantOutput output = gson.fromJson(outputJson, CookieImplantOutput.class);
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

        @SuppressWarnings("SameParameterValue")
        static Predicate<JsonElement> jsonObjectWithStringProperty(final String propertyName, Predicate<String> valuePredicate) {
            return jsonElement -> {
                if (jsonElement == null || !jsonElement.isJsonObject()) {
                    return false;
                }
                JsonObject obj = jsonElement.getAsJsonObject();
                JsonElement property = obj.get(propertyName);
                if (property == null || !property.isJsonPrimitive()) {
                    return false;
                }
                if (property.getAsJsonPrimitive().isString()) {
                    return valuePredicate.test(property.getAsString());
                }
                return false;
            };
        }

        static By elementTextIsJson(By elementLocator, Predicate<JsonElement> jsonPredicate) {
            final JsonParser parser = new JsonParser();
            return Bys.elementWithText(elementLocator, t -> {
                if (t == null) {
                    return false;
                }
                JsonElement element = parser.parse(t);
                return jsonPredicate.test(element);
            });
        }

        protected static JsonObject waitForCookieImplantOutput(WebDriver driver, int timeOutInSeconds) {
            WebElement outputElement = new WebDriverWait(driver, timeOutInSeconds).until(ExpectedConditions.presenceOfElementLocated(elementTextIsJson(By.cssSelector("#output"), jsonObjectWithStringProperty("status", "all_imports_processed"::equals))));
            String outputJson = outputElement.getText();
            if (outputJson.trim().isEmpty()) {
                throw new CookieImplantException("output empty");
            }
            JsonObject output = new JsonParser().parse(outputJson).getAsJsonObject();
            return output;
        }

        protected final Function<DeserializableCookie, String> cookieJsonTransform = new Function<DeserializableCookie, String>() {

            private void maybeAdd(JsonObject object, String field, @Nullable String value) {
                maybeAdd(object, field, value == null ? null : new JsonPrimitive(value));
            }

            private void maybeAdd(JsonObject object, String field, @Nullable Number value) {
                maybeAdd(object, field, value == null ? null : new JsonPrimitive(value));
            }

            private void maybeAdd(JsonObject object, String field, @Nullable JsonElement value) {
                if (value != null && !value.isJsonNull()) {
                    object.add(field, value);
                }
            }

            // for cookie object definition, see https://developer.chrome.com/extensions/cookies#method-set
            @Override
            public String apply(DeserializableCookie c) {
                JsonObject j = new JsonObject();
                j.addProperty("domain", checkNotNull(c.getBestDomainProperty(), "domain not set on cookie"));
                j.addProperty("url", guessUrlFromDomain(c).toString());
                maybeAdd(j, "name", c.getName());
                maybeAdd(j, "value", c.getValue());
                j.addProperty("secure", c.isSecure());
                j.addProperty("httpOnly", c.isHttpOnly());
                Date expiry = c.getExpiryDate();
                if (expiry != null) {
                    long expiryInSeconds = expiry.getTime() / 1000;
                    j.addProperty("expirationDate", expiryInSeconds);
                }
                return gson.toJson(j);
            }

            protected URI guessUrlFromDomain(DeserializableCookie cookie) {
                String domain = cookie.getBestDomainProperty();
                if (domain == null) {
                    throw new IllegalArgumentException("domain or domain attribute must be set on cookie");
                }
                domain = CharMatcher.is('.').trimLeadingFrom(domain);
                String scheme = cookie.isSecure() ? "https" : "http";
                try {
                    return new URI(scheme, domain, "/", null);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("domain probably invalid: " + StringUtils.abbreviate(domain, 128), e);
                }
            }
        };

        protected URI buildImplantUriFromCookies(Stream<DeserializableCookie> cookies) {
            return buildImplantUriFromCookieJsons(cookies.map(cookieJsonTransform));
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
}
