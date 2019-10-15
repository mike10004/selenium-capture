package com.github.mike10004.seleniumhelp;

import com.google.common.net.HostAndPort;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;

import static org.junit.Assert.*;

public class WebdrivingConfigTest {

    @Test
    public void bypassPatterns() throws Exception {
        WebdrivingConfig config = WebdrivingConfigs.builder()
                .proxy(HostAndPort.fromParts("127.0.0.1", 46632), Arrays.asList("one", "two"))
                .build();
        ProxySpecification actual = config.getProxySpecification();
        assertNotNull(actual);
        assertTrue(actual instanceof UriProxySpecification);
        URI uri = ((UriProxySpecification)actual).getUri();
        assertEquals("specification with bypass patterns", "//127.0.0.1:46632?bypass=one&bypass=two", uri.toString());
    }

    @Test
    public void buildUriFromHostAndPort() throws Exception {
        WebdrivingConfig config = WebdrivingConfigs.builder()
                .proxy(HostAndPort.fromParts("127.0.0.1", 46632))
                .build();
        ProxySpecification specification = config.getProxySpecification();
        assertNotNull(specification);
        assertTrue(specification instanceof UriProxySpecification);
        URI uri = ((UriProxySpecification)specification).getUri();
        assertEquals("host", "127.0.0.1", uri.getHost());
        assertEquals("port", 46632, uri.getPort());
        assertNull("scheme", uri.getScheme());
    }
}