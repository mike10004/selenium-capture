package io.github.mike10004.seleniumcapture;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ListHostBypassPredicateTest {

    @Test
    public void isBypass() {

        List<String> bypassedHosts = Arrays.asList("localhost", "example.com", "127.0.0.1");

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

    @Test
    public void isBypass_portSpecified() {

        List<String> bypassedHosts = List.of("localhost:12345");

        ListHostBypassPredicate predicate = new ListHostBypassPredicate(bypassedHosts);

        for (String doBypassUri : new String[] {
                u("localhost:12345"),
        }) {
            assertTrue(doBypassUri, predicate.isBypass(doBypassUri));
        }

        for (String noBypassUri : new String[] {
                u("localhost"),
                u("example.com:12345"),
                u("localhost:80"),
                u("localhost:54321"),
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