package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbselenium.WebDriverSupport;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class FirefoxWebDriverFactory extends EnvironmentWebDriverFactory {

    private final Map<String, Object> profilePreferences;
    private final ImmutableList<DeserializableCookie> cookies;

    public FirefoxWebDriverFactory(Map<String, String> environment, Map<String, Object> profilePreferences, Iterable<DeserializableCookie> cookies) {
        this(Suppliers.ofInstance(ImmutableMap.copyOf(environment)), profilePreferences, cookies);

    }

    public FirefoxWebDriverFactory() {
        this(ImmutableMap.of(), ImmutableMap.of(), ImmutableList.of());
    }

    public FirefoxWebDriverFactory(Supplier<Map<String, String>> environmentSupplier, Map<String, Object> profilePreferences, Iterable<DeserializableCookie> cookies) {
        super(environmentSupplier);
        this.profilePreferences = ImmutableMap.copyOf(profilePreferences);
        checkPreferencesValues(this.profilePreferences.values());
        this.cookies = ImmutableList.copyOf(cookies);
    }

    protected ImmutableList<DeserializableCookie> getCookies() {
        return cookies;
    }

    @Override
    public WebDriver createWebDriver(BrowserMobProxy proxy, CertificateAndKeySource certificateAndKeySource) throws IOException {

        List<SupplementingFirefoxProfile.ProfileAction> actions = new ArrayList<>(2);
        List<DeserializableCookie> cookies_ = getCookies();
        if (!cookies.isEmpty()) {
            actions.add(new CookieInstallingProfileAction(cookies_, FirefoxCookieDb.getImporter()));
        }
        if (certificateAndKeySource instanceof FirefoxCompatibleCertificateSource) {
            ByteSource certificateDbByteSource = ((FirefoxCompatibleCertificateSource)certificateAndKeySource).getFirefoxCertificateDatabase();
            actions.add(new CertificateSupplementingProfileAction(certificateDbByteSource));

        }
        FirefoxProfile profile = new SupplementingFirefoxProfile(actions);
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
        applyAdditionalPreferences(profilePreferences, proxy, certificateAndKeySource, profile);
        FirefoxBinary binary = createFirefoxBinary();
        Map<String, String> environment = environmentSupplier.get();
        FirefoxDriver driver = WebDriverSupport.firefoxInEnvironment(environment).create(binary, profile);
        return driver;
    }

    protected void applyAdditionalPreferences(Map<String, Object> profilePreferences, BrowserMobProxy proxy, CertificateAndKeySource certificateAndKeySource, FirefoxProfile profile) {
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
            if (!preferenceValueChecker.apply(value)) {
                throw new IllegalArgumentException(String.format("preference value %s (%s) must have type that is one of %s", value, value == null ? "N/A" : value.getClass(), allowedTypes));
            }
        }
    }

    @VisibleForTesting
    static Predicate<Object> newTypePredicate(Iterable<Class<?>> permittedSuperclasses) {
        final Set<Class<?>> superclasses = ImmutableSet.copyOf(permittedSuperclasses);
        checkArgument(!superclasses.isEmpty(), "set of superclasses must be nonempty");
        return new Predicate<Object>() {
            @Override
            public boolean apply(@Nullable Object input) {
                for (Class<?> superclass : superclasses) {
                    if (superclass.isInstance(input)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    protected FirefoxBinary createFirefoxBinary() {
        return new FirefoxBinary();
    }

    private static class CertificateSupplementingProfileAction implements SupplementingFirefoxProfile.ProfileAction {

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

    static class CookieInstallingProfileAction implements SupplementingFirefoxProfile.ProfileAction {

        public static final String COOKIES_DB_FILENAME = "cookies.sqlite";

        private final List<DeserializableCookie> cookies;
        private final FirefoxCookieDb.Importer cookieImporter;

        private CookieInstallingProfileAction(List<DeserializableCookie> cookies, FirefoxCookieDb.Importer cookieImporter) {
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

    private static class SupplementingFirefoxProfile extends FirefoxProfile {

        private final ImmutableList<? extends ProfileAction> profileActions;

        protected SupplementingFirefoxProfile(Iterable<? extends ProfileAction> profileActions) {
            this.profileActions = ImmutableList.copyOf(profileActions);
        }

        interface ProfileAction {
            void perform(File profileDir);
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
            for (ProfileAction action : profileActions) {
                action.perform(profileDir);
            }
            return profileDir;
        }
    }
}
