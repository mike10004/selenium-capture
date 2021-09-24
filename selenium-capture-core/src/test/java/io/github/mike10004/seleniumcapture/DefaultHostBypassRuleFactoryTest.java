package io.github.mike10004.seleniumcapture;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class DefaultHostBypassRuleFactoryTest {

    @Test
    public void isIpAddressLiteral() {
        test(DefaultHostBypassRuleFactory::isIpAddressLiteral,
                validIpLiterals,
                concat(List.of("789.1.209.2", "1.2.3", "123"), validHostnameLiterals, validHostnamePatterns, cidrBlocks)
        );
    }

    private static final List<String> validIpLiterals = List.of("127.0.0.1", "244.3.0.1", "1.2.3.4", "[::1]", "[fe80::3]", "::1", "fe80::3", "127.0.0.1:12345", "[::1]:12345");
    private static final List<String> validHostnameLiterals = List.of("example.com", "localhost", "localhost.localdomain", "gryffindor", "localhost:12345", "example.com:12345");
    private static final List<String> validHostnamePatterns = List.of("*.google.com", "*.localhost", "*.localdomain");
    private static final List<String> cidrBlocks = List.of("127.0.0.0/8", "1.2.3.4/32", "192.168.0.0/16", "10.0.0.0/24");

    @SafeVarargs
    private static List<String> concat(List<String>...lists) {
        List<String> concatenated = new ArrayList<>();
        Arrays.stream(lists).forEach(concatenated::addAll);
        return concatenated;
    }

    @Test
    public void isCidrBlock() {
        test(DefaultHostBypassRuleFactory::isIpv4CidrBlock,
                cidrBlocks,
                concat(List.of("789.0.0.0/16", "1.1.2.3/-1", "1.2.3.4.5/32"), validIpLiterals, validHostnamePatterns, validHostnameLiterals)
        );
    }

    @Test
    public void isHostnameLiteral() {
        test(DefaultHostBypassRuleFactory::isHostnameLiteral,
                validHostnameLiterals,
                concat(List.of("exam/ple.com", "", " ", "example.", "example,", ".example.com", ".example"), validIpLiterals, cidrBlocks, validHostnamePatterns));
    }

    @Test
    public void isHostPattern() {
        test(DefaultHostBypassRuleFactory::isHostPattern, validHostnamePatterns,
                concat(List.of("*example.com", "example.*", "example.com*", "example.com.*"), validIpLiterals, cidrBlocks, validHostnameLiterals));
    }

    private void test(Predicate<String> method, Iterable<String> affirmatives, Iterable<String> negatives) {
        List<String> affirmativeFailures = new ArrayList<>();
        List<String> negativeFailures = new ArrayList<>();
        for (String aff : affirmatives) {
            if (!method.test(aff)) {
                affirmativeFailures.add(aff);
            }
        }
        for (String neg : negatives) {
            if (method.test(neg)) {
                negativeFailures.add(neg);
            }
        }
        assertEquals(method + " affirmate failures: " + affirmativeFailures + "; negative failures: " + negativeFailures,
                0, affirmativeFailures.size() + negativeFailures.size());
    }}