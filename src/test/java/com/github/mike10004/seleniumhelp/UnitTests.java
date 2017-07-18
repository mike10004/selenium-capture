package com.github.mike10004.seleniumhelp;

import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.bonigarcia.wdm.FirefoxDriverManager;

/**
 * Static constants and utility methods to assist with tests.
 */
class UnitTests {

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
    private static final String RECOMMENDED_CHROMEDRIVER_VERSION = "2.30";

    private UnitTests() {}

    public static final String RECOMMENDED_GECKODRIVER_VERSION = "0.18.0";

    /**
     * Downloads and configures the JVM for use of a recommended version of ChromeDriver.
     */
    public static void setupRecommendedChromeDriver() {
        ChromeDriverManager.getInstance().version(RECOMMENDED_CHROMEDRIVER_VERSION).setup();
    }

    /**
     * Downloads and configures the JVM for use of a recommended version of GeckoDriver.
     */
    public static void setupRecommendedGeckoDriver() {
        FirefoxDriverManager.getInstance().version(RECOMMENDED_GECKODRIVER_VERSION).setup();
    }
}
