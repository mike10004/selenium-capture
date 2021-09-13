package io.github.mike10004.seleniumcapture.firefox;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class FirefoxWebDriverFactoryBasicTest {

    @Test
    public void checkPreferencesValues() {
        FirefoxWebDriverFactory.checkPreferencesValues(ImmutableList.of()); // these are ok if no exception is thrown
        FirefoxWebDriverFactory.checkPreferencesValues(ImmutableList.of(1));
        FirefoxWebDriverFactory.checkPreferencesValues(ImmutableList.of("foo"));
        FirefoxWebDriverFactory.checkPreferencesValues(ImmutableList.of(true));
        FirefoxWebDriverFactory.checkPreferencesValues(ImmutableList.of(1, false, "bar"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkPreferencesValues_null() {
        FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList((String) null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkPreferencesValues_object() {
        FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList(new Object()));
    }

    private static class Widget {
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkPreferencesValues_widget() {
        FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList(new Widget()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkPreferencesValues_double() {
        FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList(1.5));
    }

    @Test
    public void usesEnvironment() {
        Map<String, String> expected = ImmutableMap.of("foo", "bar");
        // we don't actually launch Firefox, so we don't need to apply UnitTests.createFirefoxBinarySupplier()
        FirefoxWebDriverFactory factory = FirefoxWebDriverFactory.builder()
                .environment(expected)
                .build();
        Map<String, String> actual = factory.supplyEnvironment();
        assertEquals("environment", expected, actual);
    }
}
