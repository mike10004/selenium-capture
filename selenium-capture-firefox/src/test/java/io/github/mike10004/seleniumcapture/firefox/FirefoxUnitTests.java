package io.github.mike10004.seleniumcapture.firefox;

import com.github.mike10004.nativehelper.Platforms;
import io.github.mike10004.seleniumcapture.Subprocesses;
import io.github.mike10004.seleniumcapture.WebDriverFactory;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.github.bonigarcia.wdm.config.DriverManagerType;
import io.github.mike10004.seleniumcapture.testbases.DriverManagerSetupCache;
import io.github.mike10004.seleniumcapture.testing.UnitTests;
import io.github.mike10004.seleniumcapture.testing.WindowsUtils;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.Subprocess;
import org.junit.Assume;
import org.openqa.selenium.Platform;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.os.ExecutableFinder;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.github.mike10004.seleniumcapture.testing.UnitTests.isShowBrowserWindowEnabled;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static org.openqa.selenium.Platform.MAC;
import static org.openqa.selenium.Platform.UNIX;
import static org.openqa.selenium.Platform.WINDOWS;

public class FirefoxUnitTests {

    private static final String SETTING_FIREFOX_EXECUTABLE_PATH = "firefox.executable.path";
    private static final String ENV_FIREFOX_BIN = "FIREFOX_BIN";

    @SuppressWarnings("deprecation")
    @Nullable
    static String getFirefoxExecutablePath() {
        return UnitTests.getExecutablePath(SETTING_FIREFOX_EXECUTABLE_PATH, ENV_FIREFOX_BIN, () -> {
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

    public static Supplier<FirefoxBinary> createFirefoxBinarySupplier(String...moreCommandLineOptions) {
        String executablePath = getFirefoxExecutablePath();
        if (Strings.isNullOrEmpty(executablePath)) {
            return () -> {
                FirefoxBinary b = new FirefoxBinary();
                b.addCommandLineOptions(moreCommandLineOptions);
                return b;
            };
        } else {
            File executableFile = new File(executablePath);
            if (!executableFile.isFile()) {
                throw new RuntimeException("not found: " + executablePath);
            }
            if (!executableFile.canExecute()) {
                throw new RuntimeException("not executable: " + executableFile);
            }
            return () -> {
                FirefoxBinary b = new FirefoxBinary(executableFile);
                b.addCommandLineOptions(moreCommandLineOptions);
                return b;
            };
        }
    }

    /**
     * Downloads and configures the JVM for use of a recommended version of GeckoDriver.
     */
    public static void setupRecommendedGeckoDriver() {
        DriverManagerSetupCache.doSetup(DriverManagerType.FIREFOX);
    }

    public static WebDriverFactory headlessWebDriverFactory() {
        return headlessWebDriverFactory(false);
    }

    public static WebDriverFactory headlessWebDriverFactory(boolean acceptInsecureCerts) {
        return headlessWebDriverFactoryBuilder(acceptInsecureCerts).build();
    }

    public static FirefoxWebDriverFactory.Builder headlessWebDriverFactoryBuilder() {
        return headlessWebDriverFactoryBuilder(false);
    }

    public static FirefoxWebDriverFactory.Builder headlessWebDriverFactoryBuilder(boolean acceptInsecureCerts) {
        DriverManagerSetupCache.doSetup(DriverManagerType.FIREFOX);
        return FirefoxWebDriverFactory.builder()
                .binary(createFirefoxBinarySupplier())
                .configure(o -> o.setHeadless(!isShowBrowserWindowEnabled()))
                .acceptInsecureCerts(acceptInsecureCerts)
                .putPreferences(createFirefoxPreferences());
    }

    public static void requireEsrOrUnbrandedFirefoxBinary(Supplier<FirefoxBinary> firefoxBinarySupplier) {
        boolean allowSkipTestRequiringEsr = UnitTests.Settings.getTyped("esrRequired.allowSkip", Boolean::parseBoolean, Boolean.FALSE);
        FirefoxBinary b = firefoxBinarySupplier.get();
        File f = b.getFile();
        Subprocess subprocess = Subprocess.running(f)
                .args("--full-version")
                .build();
        ProcessResult<String, String> result = Subprocesses.executeOrPropagateInterruption(subprocess, Charset.defaultCharset(), null);
        checkState(result.exitCode() == 0);
        String versionToken = Splitter.on(CharMatcher.whitespace()).splitToList(result.content().stdout()).get(2);
        boolean ok = versionToken.endsWith("esr");
        if (allowSkipTestRequiringEsr) {
            Assume.assumeTrue("expect suffix 'esr' on version", ok);
        } else {
            if (!ok) {
                throw new FirefoxEsrOrDevBuildRequiredException(versionToken);
            }
        }
    }

    private static class FirefoxEsrOrDevBuildRequiredException extends RuntimeException {
        public FirefoxEsrOrDevBuildRequiredException(String actualVersionToken) {
            super("test requires Firefox ESR, Developer Edition, or unbranded build; actual version: " + actualVersionToken);
        }
    }

    public static Map<String, Object> createFirefoxPreferences() {
        Map<String, Object> p = new HashMap<>();
        p.put("browser.chrome.site_icons", false);
        p.put("browser.chrome.favicons", false);
        // TODO support setting additional prefs from sysprops and environment
        return p;
    }

}
