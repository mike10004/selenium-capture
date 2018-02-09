package com.github.mike10004.seleniumhelp;

import com.github.mike10004.nativehelper.subprocess.ProcessResult;
import com.github.mike10004.nativehelper.subprocess.ScopedProcessTracker;
import com.github.mike10004.nativehelper.subprocess.Subprocess;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.bonigarcia.wdm.FirefoxDriverManager;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.firefox.FirefoxBinary;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String DEFAULT_RECOMMENDED_CHROMEDRIVER_VERSION = "2.33";

    private UnitTests() {}

    private static final String DEFAULT_RECOMMENDED_GECKODRIVER_VERSION = "0.19.1";

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
        String geckodriverVersion = DEFAULT_RECOMMENDED_GECKODRIVER_VERSION;
        try {
            geckodriverVersion = getRecommendedGeckodriverVersion();
        } catch (VersionMagicException e) {
            Logger.getLogger(UnitTests.class.getName()).warning("version determination failed: " + e);
        }
        FirefoxDriverManager.getInstance().version(geckodriverVersion).setup();
    }

    private static String getRecommendedGeckodriverVersion(Version firefoxVersion) {
        String geckoVersion = FirefoxGeckoVersionMapping.ffRangeToGeckoMap.get(firefoxVersion);
        if (geckoVersion == null) {
            throw new VersionMagicException("no known geckodriver version for firefox version " + firefoxVersion);
        }
        return geckoVersion;
    }

    public static String getRecommendedGeckodriverVersion() {
        Version firefoxVersion = FirefoxGeckoVersionMapping.detectFirefoxVersion();
        return getRecommendedGeckodriverVersion(firefoxVersion);
    }

    public static boolean isHeadlessChromeTestsDisabled() {
        return Boolean.parseBoolean(System.getProperty("selenium-help.chrome.headless.tests.disabled", "false"));
    }

    @SuppressWarnings("unused")
    static class VersionMagicException extends RuntimeException {
        public VersionMagicException(String message) {
            super(message);
        }

        public VersionMagicException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static class FirefoxGeckoVersionMapping {

        public static RangeMap<Version, String> ffRangeToGeckoMap = ImmutableRangeMap.<Version, String>builder()
                .put(Range.atLeast(Version.parseVersion("53.0")), "0.18.0")
                .put(Range.closedOpen(Version.parseVersion("1.0"), Version.parseVersion("53.0")), "0.14.0")
                .build();

        public static Version detectFirefoxVersion() {
            String executablePath = getFirefoxExecutablePath();
            if (executablePath == null) {
                executablePath = "firefox";
            }
            ProcessResult<String, String> result;
            try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
                result = Subprocess.running(executablePath)
                        .arg("--version")
                        .build()
                        .launcher(processTracker)
                        .outputStrings(Charset.defaultCharset())
                        .launch().await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (result.exitCode() != 0) {
                System.err.println(result.content().stderr());
                throw new VersionMagicException("firefox --version exited with code " + result.content().stderr());
            }
            String versionString = parseVersionStringFromVersionOutput(result.content().stdout());
            Version version = Version.parseVersion(versionString);
            return version;
        }

        static String parseVersionStringFromVersionOutput(String versionOutput) throws VersionMagicException {
            Pattern patt = Pattern.compile("\\b(?:(?:Firefox)|(?:Iceweasel))\\s+(\\d+(\\.[-\\w]+)*)(\\s+.*)?$");
            Matcher matcher = patt.matcher(versionOutput);
            if (!matcher.find()) {
                throw new VersionMagicException("version string not found in output " + StringUtils.abbreviate(versionOutput, 64));
            }
            return matcher.group(1);
        }
    }

    @Nullable
    private static String getFirefoxExecutablePath() {
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
