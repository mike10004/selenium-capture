package com.github.mike10004.seleniumhelp;

import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FirefoxGeckoVersionMappingTest {

    @Test
    public void detect() {
        Version version = FirefoxGeckoVersionMapping.detectFirefoxVersion();
        assertTrue("version > 45", version.getMajorVersion() > 45);
    }

    @Test
    public void parseVersionString() throws Exception {
        String[][] testCases = {
                {"Mozilla Firefox 54.0", "54.0"},
                {"Mozilla Firefox 54.0.124", "54.0.124"},
                {"Iceweasel 54.0", "54.0"},
                {"Mozilla Firefox 54.0-nightly", "54.0-nightly"},
        };
        for (String[] testCase : testCases) {
            String input = testCase[0], output = testCase[1];
            assertEquals("parse", output, FirefoxGeckoVersionMapping.parseVersionStringFromVersionOutput(input));
        }
    }

    @Test
    public void geckodriverVersionRangeMap() {
        String rec = FirefoxGeckoVersionMapping.DEFAULT_RECOMMENDED_GECKODRIVER_VERSION;
        Collection<String> versions = FirefoxGeckoVersionMapping.ffRangeToGeckoMap.asMapOfRanges().values();
        System.out.format("checking that '%s' is in %s%n", rec, versions);
        assertTrue("default recommended version " + rec + " not in versions " + versions, versions.contains(rec));
    }
}
