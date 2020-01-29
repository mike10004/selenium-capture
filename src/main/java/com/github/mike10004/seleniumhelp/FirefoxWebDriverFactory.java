package com.github.mike10004.seleniumhelp;

import com.github.mike10004.seleniumhelp.FirefoxCookieDb.Importer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class FirefoxWebDriverFactory extends CapableWebDriverFactory<FirefoxOptions> {

    private final Supplier<FirefoxBinary> binarySupplier;
    private final Map<String, Object> profilePreferences;
    private final ImmutableList<FirefoxProfileAction> profileActions;
    private final ImmutableList<FirefoxProfileFolderAction> profileFolderActions;
    private final ImmutableList<DeserializableCookie> cookies;
    private final Path scratchDir;
    private final InstanceConstructor<? extends WebDriver> constructor;
    private final java.util.logging.Level webdriverLogLevel;

    @SuppressWarnings("unused")
    public FirefoxWebDriverFactory() {
        this(builder());
    }

    protected FirefoxWebDriverFactory(Builder builder) {
        super(builder);
        this.scratchDir = requireNonNull(builder.scratchDir);
        this.binarySupplier = requireNonNull(builder.binarySupplier);
        this.profilePreferences = ImmutableMap.copyOf(builder.profilePreferences);
        checkPreferencesValues(this.profilePreferences.values());
        this.cookies = ImmutableList.copyOf(builder.cookies);
        this.profileActions = ImmutableList.copyOf(builder.profileActions);
        this.profileFolderActions = ImmutableList.copyOf(builder.profileFolderActions);
        this.constructor = requireNonNull(builder.instanceConstructor);
        this.webdriverLogLevel = builder.webdriverLogLevel;
    }

    protected ImmutableList<DeserializableCookie> getCookies() {
        return cookies;
    }

    @Override
    public WebdrivingSession startWebdriving(WebdrivingConfig config) throws IOException {
        return createWebDriverMaybeWithProxy(config);
    }

    private SupplementingFirefoxProfile createFirefoxProfile(List<FirefoxProfileFolderAction> actions) {
        return new SupplementingFirefoxProfile(actions);
    }

    private ServicedSession createWebDriverMaybeWithProxy(WebdrivingConfig config) throws IOException {
        FirefoxOptions options = populateOptions(config);
        FirefoxBinary binary = binarySupplier.get();
        Map<String, String> environment = environmentSupplier.get();
        GeckoDriverService service = new GeckoDriverService.Builder()
                .withEnvironment(environment)
                .usingFirefoxBinary(binary)
                .build();
        WebDriver driver = constructor.construct(service, options);
        return new ServicedSession(driver, service);
    }

    @VisibleForTesting
    FirefoxOptions populateOptions(WebdrivingConfig config) throws IOException {
        List<FirefoxProfileFolderAction> actions = new ArrayList<>(2);
        List<DeserializableCookie> cookies_ = getCookies();
        if (!cookies.isEmpty()) {
            actions.add(new CookieInstallingProfileAction(cookies_, FirefoxCookieDb.getImporter(), scratchDir));
        }
        actions.addAll(profileFolderActions);
        FirefoxProfile profile = createFirefoxProfile(actions);
        FirefoxProfilePreferenceConfigurator profileConfigurator = new FirefoxProfilePreferenceConfigurator();
        profileConfigurator.disableSomeMediaSupport(profile);
        profileConfigurator.avoidAutomaticConnections(profile);
        applyAdditionalPreferences(profilePreferences, config, profile);
        for (FirefoxProfileAction profileAction : profileActions) {
            profileAction.perform(profile);
        }
        FirefoxOptions options = new FirefoxOptions();
        options.setAcceptInsecureCerts(false);
        configureLogging(options);
        configureProxy(options, profile, config);
        options.setProfile(profile);
        modifyOptions(options);
        return options;
    }

    /**
     * As of v0.52, we set geckodriver log level WARN by default. The old behavior was to refrain from
     * setting the log level at all. To revert to that behavior, set this system property to {@code true}.
     */
    public static final String SYSPROP_FIREFOX_LOG_LEVEL_LEGACY_BEHAVIOR = "selenium-capture.firefox.logLevel.legacy";

    private void configureLogging(FirefoxOptions options) {
        if (webdriverLogLevel != null) {
            options.setLogLevel(FirefoxDriverLogLevel.fromLevel(webdriverLogLevel));
        } else {
            boolean legacy = Boolean.parseBoolean(System.getProperty(SYSPROP_FIREFOX_LOG_LEVEL_LEGACY_BEHAVIOR));
            if (!legacy) {
                options.setLogLevel(FirefoxDriverLogLevel.WARN);
            }
        }
    }

    private void configureProxy(FirefoxOptions options, FirefoxProfile profile, WebdrivingConfig config) {
        @Nullable WebdrivingProxyDefinition proxySpecification = config.getProxySpecification();
        new FirefoxOptionsProxyConfigurator().configureProxy(options, profile, proxySpecification);
    }

    public static class FirefoxOptionsProxyConfigurator {

        public void configureProxy(FirefoxOptions options, FirefoxProfile profile, @Nullable WebdrivingProxyDefinition proxySpecification) {
            @Nullable org.openqa.selenium.Proxy seleniumProxy = null;
            if (proxySpecification != null) {
                seleniumProxy = proxySpecification.createWebdrivingProxy();
                /*
                 * As of 2018-09-17, if you don't override this setting, Firefox defaults to
                 * bypassing the proxy for loopback addresses (or anyway, that's the behavior
                 * it exhibits). In theory, the org.openqa.selenium.Proxy object is configured
                 * to use the correct list of bypasses, but here we set it *again* in the
                 * preferences.
                 */
                List<String> proxyBypasses = SeleniumProxies.getProxyBypasses(seleniumProxy);
                overrideProxyBypasses(proxyBypasses, profile);
            }
            options.setProxy(seleniumProxy);
        }

        /**
         * Sets the Firefox preference to bypass only proxies specified in the given list.
         */
        private void overrideProxyBypasses(List<String> bypasses, FirefoxProfile profile) {
            String value;
            if (bypasses.isEmpty()) {
                value = "";
            } else {
                value = String.join(FIREFOX_PROXY_BYPASS_RULE_DELIM, bypasses);
            }
            profile.setPreference("network.proxy.no_proxies_on", value);
            /*
             * Some protected issue in the Firefox bugzilla was resolved by requiring this
             * additional preference be set. See:
             * * https://superuser.com/a/1469276/278576
             * * https://bugzilla.mozilla.org/show_bug.cgi?id=1535581
             *
             * TODO: decide whether to refrain from setting this if our bypass list actually does include any loopback address
             */
            profile.setPreference("network.proxy.allow_hijacking_localhost", true);

        }

        private static final String FIREFOX_PROXY_BYPASS_RULE_DELIM = ",";

    }

    public static class FirefoxProfilePreferenceConfigurator {

        public void disableSomeMediaSupport(FirefoxProfile profile) {
            profile.setPreference("extensions.getAddons.cache.enabled", false);
            profile.setPreference("media.gmp-gmpopenh264.enabled", false);
            profile.setPreference("browser.newtabpage.enabled", false);
            profile.setPreference("extensions.screenshots.disabled", true);
            profile.setPreference("extensions.screenshots.upload-disabled", true);
        }

        public void avoidAutomaticConnections(FirefoxProfile profile) {
            profile.setPreference("media.gmp-manager.url", "");

            // https://support.mozilla.org/en-US/kb/how-stop-firefox-making-automatic-connections?redirectlocale=en-US&redirectslug=Firefox+makes+unrequested+connections
            profile.setPreference("browser.safebrowsing.provider.mozilla.updateURL", "");
            profile.setPreference("app.update.auto", false);
            profile.setPreference("app.update.url", "");
            profile.setPreference("browser.search.geoip.url", "");
            profile.setPreference("network.prefetch-next", false);
            profile.setPreference("network.http.speculative-parallel-limit", 0);
            profile.setPreference("extensions.update.enabled", false);
            profile.setPreference("extensions.update.url", "");
            profile.setPreference("extensions.update.background.url", "");
            profile.setPreference("extensions.systemAddon.update.enabled", false);
            profile.setPreference("extensions.systemAddon.update.url", "");
            profile.setPreference("toolkit.telemetry.updatePing.enabled", false);
            profile.setPreference("services.sync.prefs.sync.browser.search.update", false);
            profile.setPreference("identity.fxaccounts.enabled", false);
            profile.setPreference("extensions.blocklist.enabled", false);
            profile.setPreference("browser.safebrowsing.downloads.remote.enabled", false);
            profile.setPreference("network.dns.disablePrefetch", true);
            profile.setPreference("browser.aboutHomeSnippets.updateUrl", "");
            profile.setPreference("browser.startup.homepage_override.mstone", "ignore");
            profile.setPreference("extensions.getAddons.cache.enabled", false);
            profile.setPreference("browser.selfsupport.url", "");
            profile.setPreference("browser.casting.enabled", false);
            profile.setPreference("network.captive-portal-service.enabled", false);

            // https://support.mozilla.org/en-US/questions/1148198
            profile.setPreference("security.ssl.enable_ocsp_stapling", false);
            // https://bugzilla.mozilla.org/show_bug.cgi?id=110161
            profile.setPreference("security.OCSP.enabled", 0);
        }
    }

    /**
     * Applies additional preferences, drawn from a map, to a profile.
     * @param profilePreferences map of profile preference settings
     * @param config session config
     * @param profile the profile
     */
    @SuppressWarnings("unused")
    protected void applyAdditionalPreferences(Map<String, Object> profilePreferences,
              WebdrivingConfig config, FirefoxProfile profile) {
        for (String key : profilePreferences.keySet()) {
            Object value = profilePreferences.get(key);
            if (value instanceof String) {
                profile.setPreference(key, (String) value);
            } else if (value instanceof Integer){
                profile.setPreference(key, (Integer) value);
            } else if (value instanceof Boolean) {
                profile.setPreference(key, (Boolean) value);
            } else {
                throw new IllegalArgumentException("preference values must be int/string/boolean");
            }
        }
    }

    private static final ImmutableSet<Class<?>> allowedTypes = ImmutableSet.of(String.class, Integer.class, Boolean.class);
    private static final Predicate<Object> preferenceValueChecker =newTypePredicate(allowedTypes);

    @VisibleForTesting
    static void checkPreferencesValues(Iterable<?> values) throws IllegalArgumentException {
        for (Object value : values) {
            if (!preferenceValueChecker.test(value)) {
                throw new IllegalArgumentException(String.format("preference value %s (%s) must have type that is one of %s", value, value == null ? "N/A" : value.getClass(), allowedTypes));
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    @VisibleForTesting
    static Predicate<Object> newTypePredicate(Iterable<Class<?>> permittedSuperclasses) {
        final Set<Class<?>> superclasses = ImmutableSet.copyOf(permittedSuperclasses);
        checkArgument(!superclasses.isEmpty(), "set of superclasses must be nonempty");
        return input -> {
            for (Class<?> superclass : superclasses) {
                if (superclass.isInstance(input)) {
                    return true;
                }
            }
            return false;
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    static class CookieInstallingProfileAction implements FirefoxProfileFolderAction {

        private static final Logger log = LoggerFactory.getLogger(CookieInstallingProfileAction.class);

        public static final String COOKIES_DB_FILENAME = "cookies.sqlite";

        private final List<DeserializableCookie> cookies;
        private final Importer cookieImporter;
        private final Path scratchDir;

        CookieInstallingProfileAction(List<DeserializableCookie> cookies, Importer cookieImporter, Path scratchDir) {
            this.cookies = requireNonNull(cookies);
            this.cookieImporter = requireNonNull(cookieImporter);
            this.scratchDir = requireNonNull(scratchDir);
        }

        @Override
        public void perform(File profileDir) {
            File sqliteDbFile = new File(profileDir, COOKIES_DB_FILENAME);
            try {
                Resources.asByteSource(getClass().getResource("/empty-firefox-cookies-db.sqlite")).copyTo(Files.asByteSink(sqliteDbFile));
                cookieImporter.importCookies(cookies, sqliteDbFile, scratchDir);
                log.debug("imported {} cookies into firefox profile sqlite database {}", cookies.size(), sqliteDbFile);
            } catch (SQLException | IOException e) {
                throw new ProfilePreparationException("failed to install cookies into " + sqliteDbFile, e);
            }
        }

    }

    /**
     * Interface defining a method that will be invoked after the profile instance
     * has been constructed and partially configured. The {@link #perform(FirefoxProfile)}
     * method of this class is passed the instance of {@link FirefoxProfile} that is used
     * to construct a {@link org.openqa.selenium.firefox.FirefoxDriver}.
     */
    public interface FirefoxProfileAction {

        /**
         * Configures a Firefox profile instance.
         * @param profile the profile instance
         * @throws IOException if an I/O error occurs
         */
        void perform(FirefoxProfile profile) throws IOException;
    }

    interface FirefoxProfileFolderAction {
        void perform(File profileDir);
        @SuppressWarnings("unused")
        class ProfilePreparationException extends IllegalStateException {
            public ProfilePreparationException() {
            }

            public ProfilePreparationException(String s) {
                super(s);
            }

            public ProfilePreparationException(String message, Throwable cause) {
                super(message, cause);
            }

            public ProfilePreparationException(Throwable cause) {
                super(cause);
            }
        }

    }

    private static final class SupplementingFirefoxProfile extends org.openqa.selenium.firefox.FirefoxProfile {

        private final ImmutableList<? extends FirefoxProfileFolderAction> profileFolderActions;

        public SupplementingFirefoxProfile(Iterable<? extends FirefoxProfileFolderAction> profileFolderActions) {
            this.profileFolderActions = ImmutableList.copyOf(profileFolderActions);
        }

        @Override
        public File layoutOnDisk() {
            File profileDir = super.layoutOnDisk();
            for (FirefoxProfileFolderAction action : profileFolderActions) {
                action.perform(profileDir);
            }
            return profileDir;
        }
    }

    public interface InstanceConstructor<T> {
        T construct(GeckoDriverService geckoDriverService, FirefoxOptions options) throws IOException;
    }

    @SuppressWarnings("unused")
    public static class Builder extends CapableWebDriverFactoryBuilder<Builder, FirefoxOptions> {

        private Supplier<FirefoxBinary> binarySupplier = FirefoxBinary::new;
        private Map<String, Object> profilePreferences = new LinkedHashMap<>();
        private List<DeserializableCookie> cookies = new ArrayList<>();
        private Path scratchDir = FileUtils.getTempDirectory().toPath();
        private List<FirefoxProfileAction> profileActions = new ArrayList<>();
        private List<FirefoxProfileFolderAction> profileFolderActions = new ArrayList<>();
        private InstanceConstructor<? extends WebDriver> instanceConstructor = FirefoxDriver::new;
        private java.util.logging.Level webdriverLogLevel = null;

        private Builder() {
        }

        public Builder webdriverLogLevel(java.util.logging.Level webdriverLogLevel) {
            this.webdriverLogLevel = webdriverLogLevel;
            return this;
        }

        /**
         * @deprecated use {@code configure(o -> o.setHeadless(true))}
         */
        @Deprecated
        public Builder headless(boolean headless) {
            return configure(o -> o.setHeadless(headless));
        }

        /**
         * @deprecated use {@code configure(o -> o.setHeadless(true))}
         */
        public Builder headless() {
            return headless(true);
        }

        public Builder constructor(InstanceConstructor<? extends WebDriver> constructor) {
            this.instanceConstructor = requireNonNull(constructor);
            return this;
        }

        public Builder binary(FirefoxBinary binary) {
            return binary(() -> binary);
        }

        public Builder binary(Supplier<FirefoxBinary> binarySupplier) {
            this.binarySupplier = requireNonNull(binarySupplier);
            return this;
        }

        /**
         * Replaces the current map of profile preferences.
         * @param val map of profile preferences
         * @return this instance
         */
        public Builder preferences(Map<String, Object> val) {
            profilePreferences = requireNonNull(val);
            return this;
        }

        /**
         * Puts all the argument preferences onto this instance's preferences map.
         * @param val preferences to put
         * @return this instance
         */
        public Builder putPreferences(Map<String, Object> val) {
            profilePreferences.putAll(val);
            return this;
        }

        /**
         * Adds one cookie.
         * @param cookie cookie to add
         * @return this instance
         * @see #scratchDir(Path)
         */
        public Builder cookie(DeserializableCookie cookie) {
            cookies.add(cookie);
            return this;
        }

        /**
         * Adds a list of cookies.
         * @param cookies cookies to add
         * @return this instance
         * @see #scratchDir(Path)
         */
        public Builder addCookies(Iterable<DeserializableCookie> cookies) {
            Iterables.addAll(this.cookies, cookies);
            return this;
        }

        /**
         * Replaces the list of cookies.
         * @param cookies cookies
         * @return this instance
         * @see #scratchDir(Path)
         */
        public Builder cookies(Iterable<DeserializableCookie> cookies) {
            this.cookies.clear();
            return addCookies(cookies);
        }

        @SuppressWarnings("BooleanParameter")
        public Builder preference(String key, boolean value) {
            profilePreferences.put(key, value);
            return this;
        }

        public Builder preference(String key, int value) {
            profilePreferences.put(key, value);
            return this;
        }

        public Builder preference(String key, String value) {
            profilePreferences.put(key, value);
            return this;
        }

        public Builder profileAction(FirefoxProfileAction profileAction) {
            profileActions.add(profileAction);
            return this;
        }

        public Builder profileFolderAction(FirefoxProfileFolderAction profileFolderAction) {
            profileFolderActions.add(profileFolderAction);
            return this;
        }

        public Builder scratchDir(Path scratchDir) {
            this.scratchDir = requireNonNull(scratchDir);
            return this;
        }

        public FirefoxWebDriverFactory build() {
            return new FirefoxWebDriverFactory(this);
        }
    }

}
