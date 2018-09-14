package com.github.mike10004.seleniumhelp;

import com.github.mike10004.seleniumhelp.FirefoxCookieDb.Importer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class FirefoxWebDriverFactory extends EnvironmentWebDriverFactory {

    private final Supplier<FirefoxBinary> binarySupplier;
    private final Map<String, Object> profilePreferences;
    private final ImmutableList<FirefoxProfileAction> profileActions;
    private final ImmutableList<FirefoxProfileFolderAction> profileFolderActions;
    private final ImmutableList<DeserializableCookie> cookies;
    private final Path scratchDir;
    private final InstanceConstructor<? extends WebDriver> constructor;

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
    }

    protected ImmutableList<DeserializableCookie> getCookies() {
        return cookies;
    }

    @Override
    public WebdrivingSession createWebdrivingSession(WebDriverConfig config) throws IOException {
        return createWebDriverMaybeWithProxy(config);
    }

    private SupplementingFirefoxProfile createFirefoxProfile(List<FirefoxProfileFolderAction> actions) {
        return new SupplementingFirefoxProfile(actions);
    }

    protected FirefoxOptions createFirefoxOptions() {
        return new FirefoxOptions();
    }

    private ServicedSession createWebDriverMaybeWithProxy(WebDriverConfig config) throws IOException {
        @Nullable InetSocketAddress proxy = config.getProxyAddress();
        @Nullable CertificateAndKeySource certificateAndKeySource = config.getCertificateAndKeySource();
        List<FirefoxProfileFolderAction> actions = new ArrayList<>(2);
        List<DeserializableCookie> cookies_ = getCookies();
        if (!cookies.isEmpty()) {
            actions.add(new CookieInstallingProfileAction(cookies_, FirefoxCookieDb.getImporter(), scratchDir));
        }
        if (certificateAndKeySource instanceof FirefoxCompatibleCertificateSource) {
            ByteSource certificateDbByteSource = ((FirefoxCompatibleCertificateSource)certificateAndKeySource).getFirefoxCertificateDatabase();
            actions.add(new CertificateSupplementingProfileAction(certificateDbByteSource));
        }
        actions.addAll(profileFolderActions);
        FirefoxProfile profile = createFirefoxProfile(actions);
        profile.setPreference("browser.aboutHomeSnippets.updateUrl", "");
        profile.setPreference("extensions.getAddons.cache.enabled", false);
        profile.setPreference("media.gmp-gmpopenh264.enabled", false);
        profile.setPreference("browser.newtabpage.enabled", false);
        profile.setPreference("app.update.url", "");
        profile.setPreference("browser.safebrowsing.provider.mozilla.updateURL", "");
        profile.setPreference("media.gmp-manager.url", "");
        if (proxy != null) {
            // https://stackoverflow.com/questions/2887978/webdriver-and-proxy-server-for-firefox
            profile.setPreference("network.proxy.type", 1);
            profile.setPreference("network.proxy.http", "localhost");
            profile.setPreference("network.proxy.http_port", proxy.getPort());
            profile.setPreference("network.proxy.ssl", "localhost");
            profile.setPreference("network.proxy.ssl_port", proxy.getPort());
            profile.setPreference("network.proxy.no_proxies_on", makeProxyBypassPreferenceValue(config.getProxyBypasses()));
            profile.setPreference("browser.search.geoip.url", "");
            profile.setPreference("network.prefetch-next", false);
            profile.setPreference("network.http.speculative-parallel-limit", 0);
        }
        applyAdditionalPreferences(profilePreferences, proxy, certificateAndKeySource, profile);
        for (FirefoxProfileAction profileAction : profileActions) {
            profileAction.perform(profile);
        }
        FirefoxBinary binary = binarySupplier.get();
        Map<String, String> environment = environmentSupplier.get();
        FirefoxOptions options = createFirefoxOptions();
        options.setProfile(profile);
        GeckoDriverService service = new GeckoDriverService.Builder()
                .withEnvironment(environment)
                .usingFirefoxBinary(binary)
                .build();
        WebDriver driver = constructor.construct(service, options);
        return new ServicedSession(driver, service);
    }

    private static final String FIREFOX_PROXY_BYPASS_RULE_DELIM = ", ";

    private String makeProxyBypassPreferenceValue(List<String> hosts) {
        if (hosts == null || hosts.isEmpty()) {
            return "";
        }
        String prefValue = hosts.stream().collect(Collectors.joining(FIREFOX_PROXY_BYPASS_RULE_DELIM));
        return prefValue;
    }

    /**
     * Applies additional preferences, drawn from a map, to a profile. Subclasses may overr
     * @param profilePreferences map of profile preference settings
     * @param proxy the proxy socket address
     * @param certificateAndKeySource the certificate and key source
     * @param profile the profile
     */
    @SuppressWarnings("unused")
    protected void applyAdditionalPreferences(Map<String, Object> profilePreferences,
              @Nullable InetSocketAddress proxy,
              @Nullable CertificateAndKeySource certificateAndKeySource, FirefoxProfile profile) {
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

    private static class CertificateSupplementingProfileAction implements FirefoxProfileFolderAction {

        public static final String CERTIFICATE_DB_FILENAME = "cert8.db";

        private final ByteSource certificateDbSource;

        private CertificateSupplementingProfileAction(ByteSource certificateDbSource) {
            super();
            this.certificateDbSource = requireNonNull(certificateDbSource);
        }

        @Override
        public void perform(File profileDir) {
            File certificateDbFile = new File(profileDir, CERTIFICATE_DB_FILENAME);
            try {
                certificateDbSource.copyTo(Files.asByteSink(certificateDbFile));
            } catch (IOException e) {
                throw new ProfilePreparationException("failed to copy certificate database to profile dir " + profileDir, e);
            }
        }
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

    private static class SupplementingFirefoxProfile extends org.openqa.selenium.firefox.FirefoxProfile {

        private final ImmutableList<? extends FirefoxProfileFolderAction> profileFolderActions;

        protected SupplementingFirefoxProfile(Iterable<? extends FirefoxProfileFolderAction> profileFolderActions) {
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
    public static class Builder extends EnvironmentWebDriverFactory.Builder<Builder> {

        private Supplier<FirefoxBinary> binarySupplier = FirefoxBinary::new;
        private Map<String, Object> profilePreferences = new LinkedHashMap<>();
        private List<DeserializableCookie> cookies = new ArrayList<>();
        private Path scratchDir = FileUtils.getTempDirectory().toPath();
        private List<FirefoxProfileAction> profileActions = new ArrayList<>();
        private List<FirefoxProfileFolderAction> profileFolderActions = new ArrayList<>();
        private InstanceConstructor<? extends WebDriver> instanceConstructor = FirefoxDriver::new;

        private Builder() {
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
