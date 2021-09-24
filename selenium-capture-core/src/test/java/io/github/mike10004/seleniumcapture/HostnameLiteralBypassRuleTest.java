package io.github.mike10004.seleniumcapture;

import com.google.common.net.HostAndPort;
import org.junit.Test;

import java.net.URI;
import java.util.List;

import static org.junit.Assert.*;

public class HostnameLiteralBypassRuleTest {


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