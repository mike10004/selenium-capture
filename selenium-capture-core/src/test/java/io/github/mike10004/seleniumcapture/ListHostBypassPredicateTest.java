package io.github.mike10004.seleniumcapture;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class ListHostBypassPredicateTest {

    @Test
    public void isBypass() {

        HostBypassRuleFactory hostBypassRuleFactory = HostBypassRuleFactory.createDefault();
        List<HostBypassRule> bypassedHosts = Stream.of("localhost", "example.com", "127.0.0.1")
                .map(hostBypassRuleFactory::fromSpec)
                .collect(Collectors.toList());

        ListHostBypassPredicate predicate = new ListHostBypassPredicate(bypassedHosts);

        for (String doBypassUri : new String[] {
                u("localhost"),
                u("LOCALHOST"),
                u("127.0.0.1"),
                u("example.com"),
        }) {
            assertTrue(doBypassUri, predicate.isBypass(doBypassUri));
        }

        for (String noBypassUri : new String[] {
                u("www.example.com"),
                u("localhost.localdomain"),
                u("127.0.5.1"),
                u("cyberbiz.net"),
        }) {
            assertFalse(noBypassUri, predicate.isBypass(noBypassUri));
        }


    }

    /**
     * Return a valid HTTP URL with the given host.
     * @param host
     * @return
     */
    private static String u(String host) {
        return String.format("https://%s/index.html", host);
    }
}