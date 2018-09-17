package com.github.mike10004.seleniumhelp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class HostBypassTestCase {

    public final List<String> preconfigured;

    public final List<String> specifiedBySessionConfig;

    public final Set<String> expected;

    public HostBypassTestCase(Collection<String> preconfigured, Collection<String> specifiedBySessionConfig) {
        this.preconfigured = new ArrayList<>(preconfigured);
        this.specifiedBySessionConfig = new ArrayList<>(specifiedBySessionConfig);
        expected = Stream.concat(preconfigured.stream(), specifiedBySessionConfig.stream()).collect(Collectors.toSet());
    }

    public static List<HostBypassTestCase> all() {
        Collection<String> singleton = Collections.singleton("one");
        Collection<String> doubleton = Arrays.asList("two", "three");
        Collection<String> empty = Collections.emptyList();
        //noinspection RedundantArrayCreation
        return Arrays.asList(new HostBypassTestCase[]{
                new HostBypassTestCase(Collections.emptyList(), Collections.emptyList()),
                new HostBypassTestCase(empty, singleton),
                new HostBypassTestCase(singleton, empty),
                new HostBypassTestCase(empty, doubleton),
                new HostBypassTestCase(doubleton, empty),
                new HostBypassTestCase(singleton, doubleton),
                new HostBypassTestCase(doubleton, singleton),
        });
    }

    public String toString() {
        return String.format("TestCase{pre=%s,session=%s,expected=%s}", preconfigured, specifiedBySessionConfig, expected);
    }

    public static void runAll(Function<HostBypassTestCase, List<String>> actualizer) {
        List<HostBypassTestCase> failures = new ArrayList<>();
        List<HostBypassTestCase> testCases = HostBypassTestCase.all();
        for (HostBypassTestCase testCase : testCases) {
            List<String> actual = actualizer.apply(testCase);
            if (!testCase.expected.equals(new HashSet<>(actual))) {
                System.out.format("%s actual = %s%n", testCase, actual);
                failures.add(testCase);
            }
        }
        assertEquals(String.format("%d failures in %d test cases", failures.size(), testCases.size()), Collections.emptyList(), failures);
    }
}
