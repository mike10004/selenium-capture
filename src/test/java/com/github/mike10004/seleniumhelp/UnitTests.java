package com.github.mike10004.seleniumhelp;

import com.github.mike10004.nativehelper.Platforms;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.bonigarcia.wdm.FirefoxDriverManager;
import org.jsoup.Jsoup;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.internal.Executable;
import org.openqa.selenium.os.ExecutableFinder;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.openqa.selenium.Platform.MAC;
import static org.openqa.selenium.Platform.UNIX;
import static org.openqa.selenium.Platform.WINDOWS;
import static org.openqa.selenium.os.WindowsUtils.getPathsInProgramFiles;

/**
 * Static constants and utility methods to assist with tests.
 */
public class UnitTests {

    private static final String SYSPROP_CHROME_OPTIONS_EXTRA_ARGS = "selenium-help.chrome.options.extraArgs";
    private static final String SYSPROP_FIREFOX_EXECUTABLE_PATH = "selenium-help.firefox.executable.path";
    private static final String SYSPROP_CHROME_EXECUTABLE_PATH = "selenium-help.chrome.executable.path";
    private static final String SYSPROP_CHROMEDRIVER_VERSION = "selenium-help.chromedriver.version";
    public static final String SYSPROP_OPENSSL_TESTS_SKIP = "selenium-help.openssl.tests.skip";
    private static final String SYSPROP_OPENSSL_EXECUTABLE = "selenium-help.openssl.executable.path";
    private static final String SYSPROP_CHROME_HEADLESS_TESTS_DISABLED = "selenium-help.chrome.headless.tests.disabled";

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
    private static final String DEFAULT_RECOMMENDED_CHROMEDRIVER_VERSION_ = "2.39";

    private UnitTests() {}

    private static String getRecommendedChromedriverVersion() {
        String val = System.getProperty(SYSPROP_CHROMEDRIVER_VERSION);
        if (Strings.isNullOrEmpty(val)) {
            val = DEFAULT_RECOMMENDED_CHROMEDRIVER_VERSION_;
        }
        return val;
    }

    /**
     * Downloads and configures the JVM for use of a recommended version of ChromeDriver.
     */
    public static void setupRecommendedChromeDriver() {
        String version = getRecommendedChromedriverVersion();
        ChromeDriverManager.getInstance().version(version).setup();
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
        return Boolean.parseBoolean(System.getProperty(SYSPROP_CHROME_HEADLESS_TESTS_DISABLED, "false"));
    }

    @Nullable
    static String getFirefoxExecutablePath() {
        return getExecutablePath(SYSPROP_FIREFOX_EXECUTABLE_PATH, "FIREFOX_BIN", () -> {
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
        return getExecutablePath(SYSPROP_CHROME_EXECUTABLE_PATH, "CHROME_BIN", () -> null);
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
     *
     * Copied from Selenium's FirefoxBinary.java. See license above.
     */
   private static Stream<Executable> locateFirefoxBinariesFromPlatform() {
        ImmutableList.Builder<Executable> executables = new ImmutableList.Builder<>();

        Platform current = Platform.getCurrent();
        if (current.is(WINDOWS)) {
            executables.addAll(Stream.of(getPathsInProgramFiles("Mozilla Firefox\\firefox.exe"),
                    getPathsInProgramFiles("Firefox Developer Edition\\firefox.exe"),
                    getPathsInProgramFiles("Nightly\\firefox.exe"))
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
     * Creates a Chrome options object suitable for unit tests.
     * @return
     */
    public static ChromeOptions createChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        String executablePath = getChromeExecutablePath();
        if (executablePath != null) {
            options.setBinary(executablePath);
        }
        options.addArguments(getChromeOptionsExtraArgs());
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
}
