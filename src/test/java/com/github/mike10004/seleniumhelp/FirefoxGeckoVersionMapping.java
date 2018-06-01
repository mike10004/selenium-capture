package com.github.mike10004.seleniumhelp;

import com.github.mike10004.nativehelper.subprocess.ProcessResult;
import com.github.mike10004.nativehelper.subprocess.Subprocess;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FirefoxGeckoVersionMapping {

    static final String DEFAULT_RECOMMENDED_GECKODRIVER_VERSION = "0.20.1";

    public static final RangeMap<Version, String> ffRangeToGeckoMap = ImmutableRangeMap.<Version, String>builder()
            .put(Range.atLeast(Version.parseVersion("53.0")), DEFAULT_RECOMMENDED_GECKODRIVER_VERSION)
            .put(Range.closedOpen(Version.parseVersion("1.0"), Version.parseVersion("53.0")), "0.14.0")
            .build();

    public static Version detectFirefoxVersion() {
        String executablePath = UnitTests.getFirefoxExecutablePath();
        if (executablePath == null) {
            executablePath = "firefox";
        }
        ProcessResult<String, String> result;
        try {
            result = Subprocesses.executeAndWait(Subprocess.running(executablePath).arg("--version").build(), Charset.defaultCharset(), null);
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

    @SuppressWarnings("unused")
    static class VersionMagicException extends RuntimeException {
        public VersionMagicException(String message) {
            super(message);
        }

        public VersionMagicException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static String getRecommendedGeckodriverVersion(Version firefoxVersion) {
        String geckoVersion = FirefoxGeckoVersionMapping.ffRangeToGeckoMap.get(firefoxVersion);
        if (geckoVersion == null) {
            throw new VersionMagicException("no known geckodriver version for firefox version " + firefoxVersion);
        }
        return geckoVersion;
    }

    public static String getRecommendedGeckodriverVersion() {
        try {
            Version firefoxVersion = FirefoxGeckoVersionMapping.detectFirefoxVersion();
            return getRecommendedGeckodriverVersion(firefoxVersion);
        } catch (VersionMagicException e) {
            Logger.getLogger(FirefoxGeckoVersionMapping.class.getName()).warning("version determination failed: " + e);
            return DEFAULT_RECOMMENDED_GECKODRIVER_VERSION;
        }
    }

}
