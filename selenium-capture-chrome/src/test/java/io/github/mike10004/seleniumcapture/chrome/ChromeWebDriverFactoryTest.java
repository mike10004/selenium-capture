package io.github.mike10004.seleniumcapture.chrome;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ChromeWebDriverFactoryTest {

    @Test
    public void usesEnvironment() {
        Map<String, String> expected = ImmutableMap.of("foo", "bar");
        ChromeWebDriverFactory factory = ChromeWebDriverFactory.builder()
                .environment(expected)
                .build();
        Map<String, String> actual = factory.supplyEnvironment();
        assertEquals("environment", expected, actual);
    }

    @SuppressWarnings({"SameParameterValue", "unused"})
    private static String[] append(Iterable<String> firsts, String last) {
        return Iterables.toArray(Iterables.concat(firsts, Collections.singleton(last)), String.class);
    }

    @Test
    public void getArguments() {
        ChromeOptions options = new ChromeOptions();
        List<String> expected = Arrays.asList("a", "b", "c");
        options.addArguments(expected);
        List<String> actual = ChromeWebDriverFactory.getArguments(options);
        assertEquals("args", expected, actual);
    }

}