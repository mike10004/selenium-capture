package com.github.mike10004.seleniumhelp;

import com.github.mike10004.nativehelper.Platforms;
import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.mike10004.nitsick.SettingSet;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.os.ExecutableFinder;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final String SYSPROP_CHROME_OPTIONS_EXTRA_ARGS = "selenium-help.chrome.options.extraArgs";
    private static final String SYSPROP_FIREFOX_EXECUTABLE_PATH = "selenium-help.firefox.executable.path";
    private static final String SYSPROP_CHROME_EXECUTABLE_PATH = "selenium-help.chrome.executable.path";
    public static final String SYSPROP_OPENSSL_TESTS_SKIP = "selenium-help.openssl.tests.skip";
    private static final String SYSPROP_OPENSSL_EXECUTABLE = "selenium-help.openssl.executable.path";
    private static final String SYSPROP_CHROME_HEADLESS_TESTS_DISABLED = "selenium-help.chrome.headless.tests.disabled";
    private static final String SYSPROP_DEBUG_ENVIRONMENT = "selenium-help.build.environment.debug";
    private static final String ENV_FIREFOX_BIN = "FIREFOX_BIN";
    private static final String ENV_CHROME_BIN = "CHROME_BIN";

    private UnitTests() {}

    @SuppressWarnings("SameParameterValue")
    private static void print(String key, @Nullable String value, PrintStream out) {
        value = value == null ? "" : StringEscapeUtils.escapeJava(value);
        out.format("%s=%s%n", key, value);
    }

    static {
        if (Boolean.parseBoolean(System.getProperty(SYSPROP_DEBUG_ENVIRONMENT))) {
            System.err.format("%s=true; describing build environment...%n%n", SYSPROP_DEBUG_ENVIRONMENT);
            System.err.format("environment variables:%n%n");
            for (String envVarName : new String[]{"CHROMEDRIVER_VERSION", "GECKODRIVER_VERSION", "DISPLAY", ENV_CHROME_BIN, ENV_FIREFOX_BIN}) {
                print(envVarName, System.getenv(envVarName), System.err);
            }
            System.err.format("%nsystem properties:%n%n");
            for (String syspropName : new String[]{
                    SYSPROP_CHROME_OPTIONS_EXTRA_ARGS,
                    SYSPROP_FIREFOX_EXECUTABLE_PATH,
                    SYSPROP_CHROME_EXECUTABLE_PATH,
                    SYSPROP_OPENSSL_TESTS_SKIP,
                    SYSPROP_OPENSSL_EXECUTABLE,
                    SYSPROP_CHROME_HEADLESS_TESTS_DISABLED,
                    "wdm.chromeDriverVersion",
                    "wdm.geckoDriverVersion",
            }) {
                String value = System.getProperty(syspropName);
                print(syspropName, value, System.err);
            }
            System.err.println();
        }
    }

    /**
     * Downloads and configures the JVM for use of a recommended version of ChromeDriver.
     */
    public static void setupRecommendedChromeDriver() {
        String value = System.getProperty("wdm.chromeDriverManager");
        System.out.format("wdm.chromeDriverManager=%s%n", value);
        WebDriverManager.chromedriver().version(value).setup(); // use system property wdm.chromeDriverManager to specify a chromedriver version
    }

    /**
     * Downloads and configures the JVM for use of a recommended version of GeckoDriver.
     */
    public static void setupRecommendedGeckoDriver() {
        WebDriverManager.firefoxdriver().setup();
    }

    public static boolean isHeadlessChromeTestsDisabled() {
        return Boolean.parseBoolean(System.getProperty(SYSPROP_CHROME_HEADLESS_TESTS_DISABLED, "false"));
    }

    @SuppressWarnings("deprecation")
    @Nullable
    static String getFirefoxExecutablePath() {
        return getExecutablePath(SYSPROP_FIREFOX_EXECUTABLE_PATH, ENV_FIREFOX_BIN, () -> {
            if (Platforms.getPlatform().isWindows()) {
                Stream<Executable> executables = locateFirefoxBinariesFromPlatform();
                File file = executables.map(Executable::getFile).filter(File::isFile).findFirst().orElse(null);
                if (file != null) {
                    return file.getAbsolutePath();
                }
            }
            return null;
        });
    }

    /**
     * Gets an executable path.
     * @return the executable path, or whatever is supplied by the defaulter; can be null if the defaulter returns null
     */
    private static String getExecutablePath(String systemPropertyName, String environmentVariableName, Supplier<String> defaulter) {
        String executablePath = Strings.emptyToNull(System.getProperty(systemPropertyName));
        if (executablePath == null && environmentVariableName != null) {
            executablePath = Strings.emptyToNull(System.getenv(environmentVariableName));
        }
        if (executablePath == null) {
            executablePath = defaulter.get();
        }
        return Strings.emptyToNull(executablePath);
    }

    @Nullable
    static String getChromeExecutablePath() {
        return getExecutablePath(SYSPROP_CHROME_EXECUTABLE_PATH, ENV_CHROME_BIN, () -> null);
    }

    // Licensed to the Software Freedom Conservancy (SFC) under one
    // or more contributor license agreements.  See the NOTICE file
    // distributed with this work for additional information
    // regarding copyright ownership.  The SFC licenses this file
    // to you under the Apache License, Version 2.0 (the
    // "License"); you may not use this file except in compliance
    // with the License.  You may obtain a copy of the License at
    //
    //   http://www.apache.org/licenses/LICENSE-2.0
    //
    // Unless required by applicable law or agreed to in writing,
    // software distributed under the License is distributed on an
    // "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    // KIND, either express or implied.  See the License for the
    // specific language governing permissions and limitations
    // under the License.

    /**
     * Locates the firefox binary by platform.
     * <p>
     * Copied from Selenium's FirefoxBinary.java. See license above.
     */
    @SuppressWarnings("deprecation")
    private static Stream<Executable> locateFirefoxBinariesFromPlatform() {
        ImmutableList.Builder<Executable> executables = new ImmutableList.Builder<>();

        Platform current = Platform.getCurrent();
        if (current.is(WINDOWS)) {
            executables.addAll(Stream.of(WindowsUtils.getPathsInProgramFiles("Mozilla Firefox\\firefox.exe"),
                    WindowsUtils.getPathsInProgramFiles("Firefox Developer Edition\\firefox.exe"),
                    WindowsUtils.getPathsInProgramFiles("Nightly\\firefox.exe"))
                    .flatMap(List::stream)
                    .map(File::new).filter(File::exists)
                    .map(Executable::new).collect(toList()));

        } else if (current.is(MAC)) {
            // system
            File binary = new File("/Applications/Firefox.app/Contents/MacOS/firefox-bin");
            if (binary.exists()) {
                executables.add(new Executable(binary));
            }

            // user home
            binary = new File(System.getProperty("user.home") + binary.getAbsolutePath());
            if (binary.exists()) {
                executables.add(new Executable(binary));
            }

        } else if (current.is(UNIX)) {
            String systemFirefoxBin = new ExecutableFinder().find("firefox-bin");
            if (systemFirefoxBin != null) {
                executables.add(new Executable(new File(systemFirefoxBin)));
            }
        }

        String systemFirefox = new ExecutableFinder().find("firefox");
        if (systemFirefox != null) {
            Path firefoxPath = new File(systemFirefox).toPath();
            if (Files.isSymbolicLink(firefoxPath)) {
                try {
                    Path realPath = firefoxPath.toRealPath();
                    File attempt1 = realPath.getParent().resolve("firefox").toFile();
                    if (attempt1.exists()) {
                        executables.add(new Executable(attempt1));
                    } else {
                        File attempt2 = realPath.getParent().resolve("firefox-bin").toFile();
                        if (attempt2.exists()) {
                            executables.add(new Executable(attempt2));
                        }
                    }
                } catch (IOException e) {
                    // ignore this path
                }

            } else {
                executables.add(new Executable(new File(systemFirefox)));
            }
        }

        return executables.build().stream();
    }

    public static Supplier<FirefoxBinary> createFirefoxBinarySupplier() {
        String executablePath = getFirefoxExecutablePath();
        if (Strings.isNullOrEmpty(executablePath)) {
            return FirefoxBinary::new;
        } else {
            File executableFile = new File(executablePath);
            if (!executableFile.isFile()) {
                throw new RuntimeException("not found: " + executablePath);
            }
            if (!executableFile.canExecute()) {
                throw new RuntimeException("not executable: " + executableFile);
            }
            return () -> new FirefoxBinary(executableFile);
        }
    }

    private static List<String> getChromeOptionsExtraArgs() {
        String tokensStr = System.getProperty(SYSPROP_CHROME_OPTIONS_EXTRA_ARGS);
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
     * system property {@link #SYSPROP_CHROME_EXECUTABLE_PATH} or environment variable
     * {@link #ENV_CHROME_BIN}.
     * @return an options object with parameters set
     */
    public static ChromeOptions createChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        String executablePath = getChromeExecutablePath();
        if (executablePath != null) {
            options.setBinary(executablePath);
        }
        options.addArguments(getChromeOptionsExtraArgs());
        options.addArguments("--disable-background-networking");
        return options;
    }

    public static ExecutableConfig makeOpensslConfig() {
        String path = Strings.emptyToNull(System.getProperty(SYSPROP_OPENSSL_EXECUTABLE));
        if (path != null) {
            File file = new File(path);
            System.out.format("using openssl executable at %s%n", file);
            return ExecutableConfig.byPathOnly(file);
        }
        return ExecutableConfig.byNameOnly("openssl");
    }

    public static ExecutableConfig makeKeytoolConfig() {
        return ExecutableConfig.byNameOnly("keytool");
    }

    public static boolean isSkipOpensslTests() {
        return Boolean.parseBoolean(System.getProperty(SYSPROP_OPENSSL_TESTS_SKIP, "false"));
    }

    public static String removeHtmlWrapping(String html) {
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        return doc.text();
    }

    public static boolean isShowBrowserWindowEnabled() {
        return Settings.get("showBrowserWindow", false);
    }

    public static XvfbRule.Builder xvfbRuleBuilder() {
        return XvfbRule.builder().disabled(UnitTests::isShowBrowserWindowEnabled);
    }

    private static final String PROPKEY_DOMAIN = "selenium-help.tests";

    public static final SettingSet Settings = SettingSet.global(PROPKEY_DOMAIN);

    public static Map<String, Object> createFirefoxPreferences() {
        Map<String, Object> p = new HashMap<>();
        // TODO support setting additional prefs from sysprops and environment
        return p;
    }

    static byte[] loadBrotliUncompressedSample() throws IOException {
        return Resources.toByteArray(BrAwareServerResponseCaptureFilterTest.class.getResource("/brotli/a100.txt"));
    }

    static byte[] loadBrotliCompressedSample() throws IOException {
        return Resources.toByteArray(BrAwareServerResponseCaptureFilterTest.class.getResource("/brotli/a100.txt.br"));
    }
}
