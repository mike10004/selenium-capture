package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import org.junit.Test;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class FirefoxWebDriverFactoryTest {

    @Test
    public void checkPreferencesValues() {
        FirefoxWebDriverFactory.checkPreferencesValues(ImmutableList.of()); // these are ok if no exception is thrown
        FirefoxWebDriverFactory.checkPreferencesValues(ImmutableList.of(1));
        FirefoxWebDriverFactory.checkPreferencesValues(ImmutableList.of("foo"));
        FirefoxWebDriverFactory.checkPreferencesValues(ImmutableList.of(true));
        FirefoxWebDriverFactory.checkPreferencesValues(ImmutableList.of(1, false, "bar"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void checkPreferencesValues_null() {
        FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList((String) null));
    }

    @Test(expected=IllegalArgumentException.class)
    public void checkPreferencesValues_object() {
        FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList(new Object()));
    }

    private static class Widget {}

    @Test(expected=IllegalArgumentException.class)
    public void checkPreferencesValues_widget() {
        FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList(new Widget()));
    }

    @Test(expected=IllegalArgumentException.class)
    public void checkPreferencesValues_dobule() {
        FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList(1.5));
    }

    @Test
    public void usesEnvironment() {
        Map<String, String> expected = ImmutableMap.of("foo", "bar");
        FirefoxWebDriverFactory factory = FirefoxWebDriverFactory.builder().environment(expected).build();
        Map<String, String> actual = factory.environmentSupplier.get();
        assertEquals("environment", expected, actual);
    }

    @Test
    public void proxyBypassList() throws Exception {
        HostBypassTestCase.runAll(this::testProxyBypass);
    }

    private List<String> testProxyBypass(HostBypassTestCase testCase) {
        FirefoxWebDriverFactory factory = FirefoxWebDriverFactory.builder()
                .preference(FirefoxWebDriverFactory.PREF_PROXY_HOST_BYPASSES, FirefoxWebDriverFactory.makeProxyBypassPreferenceValue(new HashMap<>(), testCase.preconfigured))
                .build();
        WebdrivingConfig config = WebdrivingConfig.builder()
                .proxy(HostAndPort.fromString("somewhere:1234"), testCase.specifiedBySessionConfig)
                .build();
        FirefoxOptions opts = null;
        try {
            opts = factory.populateOptions(config);
        } catch (IOException e) {
            fail("could not populate options: " + e.toString());
        }
        FirefoxProfile prof = opts.getProfile();
        assertNotNull("profile not created", prof);
        return FirefoxWebDriverFactory.parseProxyBypassPatterns(prof.getStringPreference(FirefoxWebDriverFactory.PREF_PROXY_HOST_BYPASSES, ""));
    }
}