package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.net.HostAndPort;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Test
    public void configureProxy() {
        HostBypassTestCase.runAll(this::testConfigureProxyHostBypassList);
    }

    private static final String BYPASS_ARG_PREFIX = ChromeWebDriverFactory.BYPASS_ARG_PREFIX;

    private List<String> testConfigureProxyHostBypassList(HostBypassTestCase testCase) {
        ChromeOptions options = new ChromeOptions();
        if (!testCase.preconfigured.isEmpty()) {
            options.addArguments(BYPASS_ARG_PREFIX + testCase.preconfigured.stream().collect(Collectors.joining(ChromeWebDriverFactory.proxyBypassPatternArgDelimiter())));
        }
        WebdrivingConfig config = WebdrivingConfig.builder()
                .proxy(HostAndPort.fromString("somewhere:1234"), testCase.specifiedBySessionConfig)
                .build();
        ChromeWebDriverFactory factory = new ChromeWebDriverFactory();
        factory.configureProxy(options, config);
        List<String> args = ChromeWebDriverFactory.getArguments(options);
        List<String> bypassArgs = args.stream()
                .filter(arg -> arg.startsWith(BYPASS_ARG_PREFIX))
                .map(arg -> StringUtils.removeStart(arg, BYPASS_ARG_PREFIX))
                .collect(Collectors.toList());
        if (bypassArgs.isEmpty()) {
            return Collections.emptyList();
        } else {
            String argValue = bypassArgs.get(bypassArgs.size() - 1); // the driver service only includes the last repeated --proxy-bypass-list argument
            String[] splitted = argValue.split(ChromeWebDriverFactory.proxyBypassPatternArgDelimiter());
            return Arrays.asList(splitted);
        }
    }

}