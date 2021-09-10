package io.github.mike10004.seleniumcapture.chrome;

import io.github.mike10004.seleniumcapture.testbases.DriverManagerSetupCache;
import io.github.mike10004.seleniumcapture.testing.UnitTests;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class ChromeUnitTests {

    private static final String SETTING_CHROME_OPTIONS_EXTRA_ARGS = "chrome.options.extraArgs";
    private static final String SETTING_CHROME_EXECUTABLE_PATH = "chrome.executable.path";
    private static final String SETTING_CHROME_HEADLESS_TESTS_DISABLED = "chrome.headless.tests.disabled";
    private static final String ENV_CHROME_BIN = "CHROME_BIN";

    /**
     * Downloads and configures the JVM for use of a recommended version of ChromeDriver.
     */
    public static void setupRecommendedChromeDriver() {
        DriverManagerSetupCache.doSetup(DriverManagerType.CHROME);
    }

    public static boolean isHeadlessChromeTestsDisabled() {
        return UnitTests.Settings.get(SETTING_CHROME_HEADLESS_TESTS_DISABLED, false);
    }

    @Nullable
    static String getChromeExecutablePath() {
        return UnitTests.getExecutablePath(SETTING_CHROME_EXECUTABLE_PATH, ENV_CHROME_BIN, () -> null);
    }

    private static List<String> getChromeOptionsExtraArgs() {
        String tokensStr = UnitTests.Settings.get(SETTING_CHROME_OPTIONS_EXTRA_ARGS);
        return getChromeOptionsExtraArgs(tokensStr);
    }

    private static final Splitter chromeExtraArgSplitter = Splitter.on(CharMatcher.breakingWhitespace()).trimResults().omitEmptyStrings();

    @VisibleForTesting
    static ImmutableList<String> getChromeOptionsExtraArgs(@Nullable String systemPropertyValue) {
        if (systemPropertyValue == null) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(chromeExtraArgSplitter.split(systemPropertyValue));
    }

    /**
     * Creates a Chrome options object suitable for unit tests. Some build environments
     * (I'm looking at you, Travis) require some tweaks to the way Chrome is executed,
     * and this allows you to specify those tweaks with a system property. The value
     * of the property is tokenized on breaking whitespace, so there's no way to include
     * an actual space within an argument, but the need for that completeness is uncommon
     * enough that we'll ignore it for now. This also sets the Chrome executable from
     * system property or environment variable.
     * @return an options object with parameters set
     * @see #SETTING_CHROME_EXECUTABLE_PATH
     * @see #ENV_CHROME_BIN
     */
    public static Consumer<ChromeOptions> createChromeOptions() {
        return options -> {
            String executablePath = getChromeExecutablePath();
            if (executablePath != null) {
                options.setBinary(executablePath);
            }
            options.addArguments(getChromeOptionsExtraArgs());
            options.addArguments("--disable-background-networking");
        };
    }

}
