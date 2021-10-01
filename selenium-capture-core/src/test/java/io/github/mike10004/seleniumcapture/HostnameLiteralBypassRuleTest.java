package io.github.mike10004.seleniumcapture;

import com.google.common.net.HostAndPort;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.*;

public class HostnameLiteralBypassRuleTest {

    @Test
    public void ipv6_noBracketsInRuleSpec() {
        HostBypassRule rule = new HostnameLiteralBypassRule(HostAndPort.fromParts("::1", 12345));
        URI httpRequestUri = URI.create("http://[::1]:12345/foo");
        assertTrue(rule + " matches " + httpRequestUri, rule.isBypass(httpRequestUri));
    }

    @Test
    public void ipv6_bracketsInRuleSpec() {
        HostBypassRule rule = new HostnameLiteralBypassRule(HostAndPort.fromString("[::1]:12345"));
        URI httpRequestUri = URI.create("http://[::1]:12345/foo");
        assertTrue(rule + " matches " + httpRequestUri, rule.isBypass(httpRequestUri));
    }

    @Test
    public void ipv6_noBracketsInRuleSpec_noBracketsInUri() {
        HostBypassRule rule = new HostnameLiteralBypassRule(HostAndPort.fromParts("::1", 12345));
        URI httpRequestUri = uriWithIpv6Host("::1", 12345);
        assertTrue(rule + " matches " + httpRequestUri, rule.isBypass(httpRequestUri));
    }

    @SuppressWarnings("SameParameterValue")
    private static URI uriWithIpv6Host(String host, int port) {
        try {
            return new URI("http", null, host, port, "/", null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void ipv6_bracketsInRuleSpec_noBracketsInUri() {
        HostBypassRule rule = new HostnameLiteralBypassRule(HostAndPort.fromString("[::1]:12345"));
        URI httpRequestUri = uriWithIpv6Host("::1", 12345);
        assertTrue(rule + " matches " + httpRequestUri, rule.isBypass(httpRequestUri));
    }

    @Test
    public void isBypass_portSpecified() {
        HostBypassRule bypassRule = new HostnameLiteralBypassRule(HostAndPort.fromString("localhost:12345"));
        for (URI doBypassUri : new URI[] {
                u("localhost:12345"),
        }) {
            assertTrue(doBypassUri.toString(), bypassRule.isBypass(doBypassUri));
        }

        for (URI noBypassUri : new URI[] {
                u("localhost"),
                u("example.com:12345"),
                u("localhost:80"),
                u("localhost:54321"),
        }) {
            assertFalse(noBypassUri.toString(), bypassRule.isBypass(noBypassUri));
        }


    }

    /**
     * Return a valid HTTP URL with the given host.
     * @param host
     * @return
     */
    private static URI u(String host) {
        return URI.create(String.format("https://%s/index.html", host));
    }

}