package com.github.mike10004.seleniumhelp;

import com.github.mike10004.nativehelper.Platforms;
import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.github.bonigarcia.wdm.config.DriverManagerType;
import io.github.mike10004.nitsick.SettingSet;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.os.ExecutableFinder;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.openqa.selenium.Platform.MAC;
import static org.openqa.selenium.Platform.UNIX;
import static org.openqa.selenium.Platform.WINDOWS;

/**
 * Static constants and utility methods to assist with tests.
 */
public class UnitTests {

    private static final String PROPKEY_DOMAIN = "selenium-help.tests";
    public static final SettingSet Settings = SettingSet.system(PROPKEY_DOMAIN);

    private static final String SETTING_DEBUG_ENVIRONMENT = "environment.debug";
    private static final String SETTING_OPENSSL_TESTS_SKIP = "openssl.skip";
    private static final String SETTING_OPENSSL_EXECUTABLE_PATH = "openssl.executable.path";

    private UnitTests() {}

    @SuppressWarnings("SameParameterValue")
    private static void print(String key, @Nullable String value, PrintStream out) {
        value = value == null ? "" : StringEscapeUtils.escapeJava(value);
        out.format("%s=%s%n", key, value);
    }

//    static {
//        if (Settings.get(SETTING_DEBUG_ENVIRONMENT, false)) {
//            System.err.format("%s.%s=true; describing build environment...%n%n", PROPKEY_DOMAIN, SETTING_DEBUG_ENVIRONMENT);
//            System.err.format("environment variables:%n%n");
//            for (String envVarName : new String[]{
//                    "CHROMEDRIVER_VERSION",
//                    "GECKODRIVER_VERSION",
//                    "DISPLAY",
//                    ENV_CHROME_BIN,
//                    ENV_FIREFOX_BIN,
//                    "SELENIUMHELP_TESTS_OPENSSL_EXECUTABLE_PATH"}) {
//                print(envVarName, System.getenv(envVarName), System.err);
//            }
//            System.err.format("%nsystem properties:%n%n");
//            for (String syspropName : new String[]{
//                    PROPKEY_DOMAIN + "." + SETTING_CHROME_OPTIONS_EXTRA_ARGS,
//                    PROPKEY_DOMAIN + "." + SETTING_FIREFOX_EXECUTABLE_PATH,
//                    PROPKEY_DOMAIN + "." + SETTING_CHROME_EXECUTABLE_PATH,
//                    PROPKEY_DOMAIN + "." + SETTING_OPENSSL_TESTS_SKIP,
//                    PROPKEY_DOMAIN + "." + SETTING_OPENSSL_EXECUTABLE_PATH,
//                    PROPKEY_DOMAIN + "." + SETTING_CHROME_HEADLESS_TESTS_DISABLED,
//                    "wdm.chromeDriverVersion",
//                    "wdm.geckoDriverVersion",
//            }) {
//                String value = System.getProperty(syspropName);
//                print(syspropName, value, System.err);
//            }
//            System.err.println();
//        }
//    }

    /**
     * Gets an executable path.
     * @return the executable path, or whatever is supplied by the defaulter; can be null if the defaulter returns null
     */
    public static String getExecutablePath(String settingName,
                                            String environmentVariableName,
                                            Supplier<String> defaulter) {
        String executablePath = Strings.emptyToNull(Settings.get(settingName));
        if (executablePath == null && environmentVariableName != null) {
            executablePath = Strings.emptyToNull(System.getenv(environmentVariableName));
        }
        if (executablePath == null) {
            executablePath = defaulter.get();
        }
        return Strings.emptyToNull(executablePath);
    }
    public static boolean isSkipOpensslTests() {
        return Settings.get(SETTING_OPENSSL_TESTS_SKIP, false);
    }

    public static String removeHtmlWrapping(String html) {
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        return doc.text();
    }

    private static final Duration SHOW_BROWSER_FLAG_FILE_LIFETIME = Duration.ofHours(24);

    private static boolean isShowBrowserFlagFilePresentAndNotExpired() {
        File flagFile = new File(System.getProperty("user.dir"), ".show-browser-window");
        if (flagFile.isFile()) {
            @Nullable Instant lastModified = null;
            try {
                lastModified = getFileLastModified(flagFile);
            } catch (IOException ignore) {
            }
            if (lastModified != null) {
                Instant deadline = Instant.now().minus(SHOW_BROWSER_FLAG_FILE_LIFETIME);
                return lastModified.isAfter(deadline);
            }
        }
        return false;
    }

    private static Instant getFileLastModified(File file) throws IOException {
        BasicFileAttributeView attrView = Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class);
        BasicFileAttributes attrs = attrView.readAttributes();
        FileTime lastModifiedTime = attrs.lastModifiedTime();
        long epochMilli = lastModifiedTime.toMillis();
        return Instant.ofEpochMilli(epochMilli);
    }

    public static boolean isShowBrowserWindowEnabled() {
        if (isShowBrowserFlagFilePresentAndNotExpired()) {
            return true;
        }
        return Settings.get("showBrowserWindow", false);
    }

    public static XvfbRule.Builder xvfbRuleBuilder() {
        return XvfbRule.builder()
                .disabled(UnitTests::isShowBrowserWindowEnabled);
    }

    @Nullable
    public static String getPageSourceOrNull(WebDriver driver) {
        return tryGet(driver::getPageSource, null, ignore ->{});
    }

    @Nullable
    public static String getDomOrNull(WebDriver driver) {
        return tryGet(() -> {
            return (String) ((JavascriptExecutor)driver).executeScript("return document.getElementsByTagName('html')[0].innerHTML");
        }, null, ignore -> {});
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> T tryGet(Supplier<T> getter, T defaultValue, Consumer<? super RuntimeException> errorListener) {
        try {
            return getter.get();
        } catch (RuntimeException e) {
            errorListener.accept(e);
        }
        return defaultValue;
    }

    public static void dumpState(WebDriver driver, PrintStream err) {
        err.println("============== +PAGE URL  ============");
        err.println(tryGet(driver::getCurrentUrl, null, ignore -> {}));
        err.println("============== -PAGE URL ============");
        err.println("============== +PAGE SOURCE ============");
        err.println(getPageSourceOrNull(driver));
        err.println("============== -PAGE SOURCE ============");
        err.println("============== +PAGE DOM ============");
        err.println(getDomOrNull(driver));
        err.println("============== -PAGE DOM ============");
    }

    public static Date truncateToSeconds(long millisSinceEpoch) {
        return DateUtils.truncate(new Date(millisSinceEpoch), Calendar.SECOND);
    }

}
