package com.github.mike10004.seleniumhelp;

import com.google.common.base.Strings;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.bonigarcia.wdm.FirefoxDriverManager;
import org.openqa.selenium.firefox.FirefoxBinary;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * Static constants and utility methods to assist with tests.
 */
public class UnitTests {

    private static final String SYSPROP_FIREFOX_EXECUTABLE_PATH = "selenium-help.firefox.executable.path";

    /**
     * Recommended version of ChromeDriver.
     *
     * TODO: determine this based on the version of Chrome installed
     *
     * Each ChromeDriver release (https://chromedriver.storage.googleapis.com/)
     * supports a range of Chrome versions, and a new version may not support a
     * Chrome version that the previous driver release did support. (That is, it's a
     * moving window that does not prize backward compatibility.) Therefore, we should
     * actually determine which version of the driver to use based on the version
     * of Chrome installed on the build system. Otherwise we're just hoping that
     * the latest version of Chrome is installed.
     */
    static final String DEFAULT_RECOMMENDED_CHROMEDRIVER_VERSION = "2.33";

    private UnitTests() {}

    /**
     * Downloads and configures the JVM for use of a recommended version of ChromeDriver.
     */
    public static void setupRecommendedChromeDriver() {
        ChromeDriverManager.getInstance().version(DEFAULT_RECOMMENDED_CHROMEDRIVER_VERSION).setup();
    }

    /**
     * Downloads and configures the JVM for use of a recommended version of GeckoDriver.
     */
    public static void setupRecommendedGeckoDriver() {
        String geckodriverVersion = getRecommendedGeckodriverVersion();
        FirefoxDriverManager.getInstance().version(geckodriverVersion).setup();
    }

    public static String getRecommendedGeckodriverVersion() {
        return FirefoxGeckoVersionMapping.getRecommendedGeckodriverVersion();
    }

    public static boolean isHeadlessChromeTestsDisabled() {
        return Boolean.parseBoolean(System.getProperty("selenium-help.chrome.headless.tests.disabled", "false"));
    }

    @Nullable
    static String getFirefoxExecutablePath() {
        String executablePath = Strings.emptyToNull(System.getProperty(SYSPROP_FIREFOX_EXECUTABLE_PATH));
        if (executablePath == null) {
            executablePath = System.getenv("FIREFOX_BIN");
        }
        return executablePath;
    }

    public static Supplier<FirefoxBinary> createFirefoxBinarySupplier() throws IOException {
        String executablePath = getFirefoxExecutablePath();
        if (Strings.isNullOrEmpty(executablePath)) {
            return FirefoxBinary::new;
        } else {
            File executableFile = new File(executablePath);
            if (!executableFile.isFile()) {
                throw new FileNotFoundException(executablePath);
            }
            if (!executableFile.canExecute()) {
                throw new IOException("not executable: " + executableFile);
            }
            return () -> new FirefoxBinary(executableFile);
        }
    }
}
