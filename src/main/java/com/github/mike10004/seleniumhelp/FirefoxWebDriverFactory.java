package com.github.mike10004.seleniumhelp;

import com.github.mike10004.seleniumhelp.FirefoxCookieDb.Importer;
import com.github.mike10004.seleniumhelp.FirefoxWebDriverFactory.SupplementingFirefoxProfile.ProfileFolderAction;
import com.github.mike10004.xvfbselenium.WebDriverSupport;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class FirefoxWebDriverFactory extends EnvironmentWebDriverFactory {

    private final Supplier<FirefoxBinary> binarySupplier;
    private final Map<String, Object> profilePreferences;
    private final ImmutableList<FirefoxProfileAction> profileActions;
    private final ImmutableList<DeserializableCookie> cookies;

    @SuppressWarnings("unused")
    public FirefoxWebDriverFactory() {
        this(builder());
    }

    protected FirefoxWebDriverFactory(Builder builder) {
        super(builder);
        this.binarySupplier = checkNotNull(builder.binarySupplier);
        this.profilePreferences = ImmutableMap.copyOf(builder.profilePreferences);
        checkPreferencesValues(this.profilePreferences.values());
        this.cookies = ImmutableList.copyOf(builder.cookies);
        this.profileActions = ImmutableList.copyOf(builder.profileActions);
    }

    protected ImmutableList<DeserializableCookie> getCookies() {
        return cookies;
    }

    @Override
    public WebDriver createWebDriver(WebDriverConfig config) throws IOException {
        return createWebDriverMaybeWithProxy(config.getProxyAddress(), config.getCertificateAndKeySource());
    }

    /**
     * @deprecated use {@link #createWebDriver(WebDriverConfig)} with an empty config
     */
    @Deprecated
    public final WebDriver unproxied() throws IOException {
        return createWebDriverMaybeWithProxy(null, null);
    }

    private WebDriver createWebDriverMaybeWithProxy(@Nullable InetSocketAddress proxy,
                                                    @Nullable CertificateAndKeySource certificateAndKeySource) throws IOException {
        List<ProfileFolderAction> actions = new ArrayList<>(2);
        List<DeserializableCookie> cookies_ = getCookies();
        if (!cookies.isEmpty()) {
            actions.add(new CookieInstallingProfileAction(cookies_, FirefoxCookieDb.getImporter()));
        }
        if (certificateAndKeySource instanceof FirefoxCompatibleCertificateSource) {
            ByteSource certificateDbByteSource = ((FirefoxCompatibleCertificateSource)certificateAndKeySource).getFirefoxCertificateDatabase();
            actions.add(new CertificateSupplementingProfileAction(certificateDbByteSource));
        }
        FirefoxProfile profile = new SupplementingFirefoxProfile(actions);
        profile.setPreference("browser.aboutHomeSnippets.updateUrl", "");
        profile.setPreference("extensions.getAddons.cache.enabled", false);
        profile.setPreference("media.gmp-gmpopenh264.enabled", false);
        profile.setPreference("browser.newtabpage.enabled", false);
        profile.setPreference("app.update.url", "");
        profile.setPreference("browser.safebrowsing.provider.mozilla.updateURL", "");
        if (proxy != null) {
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
        }
        applyAdditionalPreferences(profilePreferences, proxy, certificateAndKeySource, profile);
        for (FirefoxProfileAction profileAction : profileActions) {
            profileAction.perform(profile);
        }
        FirefoxBinary binary = binarySupplier.get();
        Map<String, String> environment = environmentSupplier.get();
        FirefoxDriver driver = WebDriverSupport.firefoxInEnvironment(environment).create(binary, profile);
        return driver;
    }

    /**
     * Applies additional preferences, drawn from a map, to a profile. Subclasses may overr
     * @param profilePreferences
     * @param proxy
     * @param certificateAndKeySource
     * @param profile
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

    private static class CertificateSupplementingProfileAction implements ProfileFolderAction {

        public static final String CERTIFICATE_DB_FILENAME = "cert8.db";

        private final ByteSource certificateDbSource;

        private CertificateSupplementingProfileAction(ByteSource certificateDbSource) {
            super();
            this.certificateDbSource = checkNotNull(certificateDbSource);
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

    static class CookieInstallingProfileAction implements ProfileFolderAction {

        public static final String COOKIES_DB_FILENAME = "cookies.sqlite";

        private final List<DeserializableCookie> cookies;
        private final Importer cookieImporter;

        private CookieInstallingProfileAction(List<DeserializableCookie> cookies, Importer cookieImporter) {
            this.cookies = checkNotNull(cookies);
            this.cookieImporter = checkNotNull(cookieImporter);
        }

        @Override
        public void perform(File profileDir) {
            File sqliteDbFile = new File(profileDir, COOKIES_DB_FILENAME);
            try {
                cookieImporter.importCookies(cookies, sqliteDbFile);
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
         * @throws IOException
         */
        void perform(FirefoxProfile profile) throws IOException;
    }

    protected static class SupplementingFirefoxProfile extends org.openqa.selenium.firefox.FirefoxProfile {

        private final ImmutableList<? extends ProfileFolderAction> profileFolderActions;

        protected SupplementingFirefoxProfile(Iterable<? extends ProfileFolderAction> profileFolderActions) {
            this.profileFolderActions = ImmutableList.copyOf(profileFolderActions);
        }

        interface ProfileFolderAction {
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

        @Override
        public File layoutOnDisk() {
            File profileDir = super.layoutOnDisk();
            for (ProfileFolderAction action : profileFolderActions) {
                action.perform(profileDir);
            }
            return profileDir;
        }
    }

    @SuppressWarnings("unused")
    public static class Builder extends EnvironmentWebDriverFactory.Builder<Builder> {

        private Supplier<FirefoxBinary> binarySupplier = FirefoxBinary::new;
        private Map<String, Object> profilePreferences = new LinkedHashMap<>();
        private List<DeserializableCookie> cookies = new ArrayList<>();
        private List<FirefoxProfileAction> profileActions = new ArrayList<>();

        private Builder() {
        }

        public Builder binary(FirefoxBinary binary) {
            return binary(() -> binary);
        }

        public Builder binary(Supplier<FirefoxBinary> binarySupplier) {
            this.binarySupplier = checkNotNull(binarySupplier);
            return this;
        }

        /**
         * Replaces the current map of profile preferences.
         * @param val map of profile preferences
         * @return this instance
         */
        public Builder preferences(Map<String, Object> val) {
            profilePreferences = checkNotNull(val);
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
         */
        public Builder cookie(DeserializableCookie cookie) {
            cookies.add(cookie);
            return this;
        }

        /**
         * Adds a list of cookies.
         * @param val cookies to add
         * @return this instance
         */
        public Builder addCookies(Iterable<DeserializableCookie> val) {
            Iterables.addAll(cookies, val);
            return this;
        }

        /**
         * Replaces the list of cookies.
         * @param val cookies
         * @return this instance
         */
        public Builder cookies(Iterable<DeserializableCookie> val) {
            cookies.clear();
            return addCookies(val);
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

        public FirefoxWebDriverFactory build() {
            return new FirefoxWebDriverFactory(this);
        }
    }

}
