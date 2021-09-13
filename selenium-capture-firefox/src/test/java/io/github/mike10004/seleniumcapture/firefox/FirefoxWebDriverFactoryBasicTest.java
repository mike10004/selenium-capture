package io.github.mike10004.seleniumcapture.firefox;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class FirefoxWebDriverFactoryBasicTest {

    @Test
    public void checkPreferencesValues() {
        FirefoxWebDriverFactory.checkPreferencesValues(ImmutableMap.<String, Object>of().entrySet()); // these are ok if no exception is thrown
        FirefoxWebDriverFactory.checkPreferencesValues(ImmutableMap.of("a", 1).entrySet());
        FirefoxWebDriverFactory.checkPreferencesValues(ImmutableMap.of("b", "foo").entrySet());
        FirefoxWebDriverFactory.checkPreferencesValues(ImmutableMap.of("c", true).entrySet());
        FirefoxWebDriverFactory.checkPreferencesValues(ImmutableMap.of("d", 1, "e", false, "f", "bar").entrySet());
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkPreferencesValues_null() {
        FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList(new AbstractMap.SimpleImmutableEntry<>("a", null)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkPreferencesValues_object() {
        FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList(new AbstractMap.SimpleImmutableEntry<>("a", new Object())));
    }

    private static class Widget {
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkPreferencesValues_widget() {
        FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList(new AbstractMap.SimpleImmutableEntry<>("a", new Widget())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkPreferencesValues_double() {
        FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList(new AbstractMap.SimpleImmutableEntry<>("a", 1.5)));
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
