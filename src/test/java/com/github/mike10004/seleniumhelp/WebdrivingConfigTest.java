package com.github.mike10004.seleniumhelp;

import com.google.common.net.HostAndPort;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;

import static org.junit.Assert.*;

public class WebdrivingConfigTest {

    @Test
    public void bypassPatterns() throws Exception {
        WebdrivingConfig config = WebdrivingConfig.builder()
                .proxy(HostAndPort.fromParts("127.0.0.1", 46632), Arrays.asList("one", "two"))
                .build();
        URI actual = config.getProxySpecification();
        assertNotNull(actual);
        assertEquals("path", "", actual.getPath());
        assertNull("user info", actual.getUserInfo());
        assertEquals("specification with bypass patterns", "//127.0.0.1:46632?bypass=one&bypass=two", actual.toString());

    }

    @Test
    public void buildUriFromHostAndPort() throws Exception {
        WebdrivingConfig config = WebdrivingConfig.builder()
                .proxy(HostAndPort.fromParts("127.0.0.1", 46632))
                .build();
        URI actual = config.getProxySpecification();
        assertNotNull(actual);
        assertEquals("host", "127.0.0.1", actual.getHost());
        assertEquals("port", 46632, actual.getPort());
        assertNull("scheme", actual.getScheme());
    }
}