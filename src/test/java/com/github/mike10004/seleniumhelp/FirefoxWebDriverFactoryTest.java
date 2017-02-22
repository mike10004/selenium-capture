package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

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
}