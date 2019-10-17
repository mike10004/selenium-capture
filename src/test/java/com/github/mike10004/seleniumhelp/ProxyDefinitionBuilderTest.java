package com.github.mike10004.seleniumhelp;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProxyDefinitionBuilderTest {

    @Test
    public void buildUriSpec_withBypassPatterns() throws Exception {
        UriProxySpecification uriSpec = ProxyDefinitionBuilder.through("127.0.0.1", 46632)
                .addProxyBypass("one")
                .addProxyBypass("two")
                .buildUriSpec();
        URI uri = uriSpec.getUri();
        assertEquals("specification with bypass patterns", "//127.0.0.1:46632?bypass=one&bypass=two", uri.toString());
    }

    @Test
    public void buildUriSpec_bare() throws Exception {
        URI uri = ProxyDefinitionBuilder.through("127.0.0.1", 46632).buildUriSpec().getUri();
        assertEquals("host", "127.0.0.1", uri.getHost());
        assertEquals("port", 46632, uri.getPort());
        assertEquals("uri", "//127.0.0.1:46632", uri.toString());
    }

    @Test
    public void buildUriSpec_socks5() throws Exception {
        URI uri = ProxyDefinitionBuilder.through("127.0.0.1", 46632)
                .socks5()
                .buildUriSpec().getUri();
        assertEquals("host", "127.0.0.1", uri.getHost());
        assertEquals("port", 46632, uri.getPort());
        assertEquals("uri", "socks5://127.0.0.1:46632", uri.toString());
    }


}