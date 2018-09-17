package com.github.mike10004.seleniumhelp;

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
        Map<String, String> actual = factory.environmentSupplier.get();
        assertEquals("environment", expected, actual);
    }

    @Test
    public void mixtureOfHeadlessAndOptions_1() throws Exception {
        ChromeOptions expected = new ChromeOptions();
        expected.addArguments(prepend("--foo", ChromeWebDriverFactory.Builder.HEADLESS_ARGS));
//        expected.addArguments(append(ChromeWebDriverFactory.Builder.HEADLESS_ARGS, "--foo"));
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--foo");
        ChromeOptions actual = ChromeWebDriverFactory.builder()
                .headless()
                .chromeOptions(options)
                .build().getChromeOptions();
        assertEquals("options as capabilities", expected.asMap(), actual.asMap());
    }

    @SuppressWarnings("SameParameterValue")
    private static String[] prepend(String first, Iterable<String> others) {
        return Iterables.toArray(Iterables.concat(Collections.singleton(first), others), String.class);
    }

    @SuppressWarnings({"SameParameterValue", "unused"})
    private static String[] append(Iterable<String> firsts, String last) {
        return Iterables.toArray(Iterables.concat(firsts, Collections.singleton(last)), String.class);
    }

    @Test
    public void mixtureOfHeadlessAndOptions_2() throws Exception {
        ChromeOptions expected = new ChromeOptions();
        expected.addArguments(prepend("--foo", ChromeWebDriverFactory.Builder.HEADLESS_ARGS));
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--foo");
        ChromeOptions actual = ChromeWebDriverFactory.builder()
                .chromeOptions(options)
                .headless()
                .build().getChromeOptions();
        assertEquals("options as capabilities", expected, actual);
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