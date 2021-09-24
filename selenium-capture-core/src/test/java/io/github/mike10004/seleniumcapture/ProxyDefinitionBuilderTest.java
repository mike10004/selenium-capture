package io.github.mike10004.seleniumcapture;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProxyDefinitionBuilderTest {

    @Test
    public void buildUriSpec_withBypassPatterns() throws Exception {
        ProxyDefinition uriSpec = ProxyDefinitionBuilder.through("127.0.0.1", 46632)
                .addProxyBypass("one")
                .addProxyBypass("two")
                .http();
        URI uri = ((UriProxyDefinition)uriSpec).getUri();
        assertEquals("specification with bypass patterns", "http://127.0.0.1:46632/?bypass=one&bypass=two", uri.toString());
    }

    @Test
    public void buildUriSpec_bare() throws Exception {
        URI uri = ((UriProxyDefinition)ProxyDefinitionBuilder.through("127.0.0.1", 46632).http()).getUri();
        assertEquals("host", "127.0.0.1", uri.getHost());
        assertEquals("port", 46632, uri.getPort());
        assertEquals("uri", "http://127.0.0.1:46632", uri.toString());
    }

    @Test
    public void buildUriSpec_socks5() throws Exception {
        URI uri = ((UriProxyDefinition)ProxyDefinitionBuilder.through("127.0.0.1", 46632)
                .socks5()).getUri();
        assertEquals("host", "127.0.0.1", uri.getHost());
        assertEquals("port", 46632, uri.getPort());
        assertEquals("uri", "socks5://127.0.0.1:46632", uri.toString());
    }

    @Test
    public void withBypasses() throws Exception {
        UriProxyDefinition upd = (UriProxyDefinition) ProxyDefinitionBuilder
                .through("1.1.1.1", 3128)
                .addProxyBypass("localhost")
                .socks5();
        URI uri = upd.getUri();
        List<NameValuePair> queryParams = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
        assertEquals("query", queryParams, Collections.singletonList(new BasicNameValuePair("bypass", "localhost")));
        assertEquals("selenium noproxy", "localhost", upd.createWebdrivingProxy().getNoProxy());
    }

}