package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class ChromeWebDriverFactoryTest {

    @Test
    public void usesEnvironment() {
        Map<String, String> expected = ImmutableMap.of("foo", "bar");
        ChromeWebDriverFactory factory = ChromeWebDriverFactory.builder().environment(expected).build();
        Map<String, String> actual = factory.environmentSupplier.get();
        assertEquals("environment", expected, actual);
    }
}