package io.github.mike10004.seleniumcapture;

import com.google.common.collect.Iterables;
import com.google.common.net.HttpHeaders;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.cookie.DefaultCookieSpecProvider;
import org.apache.http.impl.cookie.RFC6265LaxSpec;
import org.apache.http.impl.cookie.RFC6265StrictSpec;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DeserializableCookieCreatorTest {

    @Test
    public void testDefaultApacheParsing() throws Exception {
        CookieSpec spec = new DefaultCookieSpecProvider().create(new BasicHttpContext());
        testCookieSpec(spec);
    }

    @Test
    public void testApacheBrowserCompatSpec() throws Exception {
        @SuppressWarnings("deprecation")
        CookieSpec spec = new org.apache.http.impl.cookie.BrowserCompatSpec();
        testCookieSpec(spec);
    }

    @Test
    public void testApacheBestMatchSpec() throws Exception {
        @SuppressWarnings("deprecation")
        CookieSpec spec = new org.apache.http.impl.cookie.BestMatchSpec();
        testCookieSpec(spec);
    }

    @Test
    public void testApacheRFC6265LaxSpec() throws Exception {
        CookieSpec spec = new RFC6265LaxSpec();
        testCookieSpec(spec);
    }

    @Test
    public void testApacheRFC6265StrictSpec() throws Exception {
        CookieSpec spec = new RFC6265StrictSpec();
        testCookieSpec(spec);
    }

    private void testCookieSpec(CookieSpec spec) throws Exception {
        testCookieSpec_domainSpecified(spec);
        testCookieSpec_domainSpecifiedWithLeadingDot(spec);
        testCookieSpec_domainNotSpecified(spec);
    }

    private void testCookieSpec_domainSpecifiedWithLeadingDot(CookieSpec spec) throws MalformedURLException, MalformedCookieException {
        testCookieSpec(spec, "hello=world; Path=/; Domain=example.com", "example.com");
    }

    private void testCookieSpec_domainSpecified(CookieSpec spec) throws MalformedURLException, MalformedCookieException {
        testCookieSpec(spec, "hello=world; Path=/; Domain=.example.com", "example.com");
    }

    private void testCookieSpec_domainNotSpecified(CookieSpec spec) throws MalformedURLException, MalformedCookieException {
        testCookieSpec(spec, "hello=world; Path=/", "www.example.com");
    }

    private void testCookieSpec(CookieSpec spec, String setCookieHeaderValue, String expectedDomainValue) throws MalformedURLException, MalformedCookieException {
        CookieOrigin origin = CookieUtility.getInstance().buildCookieOrigin(new URL("https://www.example.com")).origin;
        List<org.apache.http.cookie.Cookie> cookies = spec.parse(new BasicHeader(HttpHeaders.SET_COOKIE, setCookieHeaderValue), origin);
        org.apache.http.cookie.Cookie cookie = Iterables.getOnlyElement(cookies);
        // According to https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie
        // leading dots are ignored; subdomains ALWAYS implied by domain
        assertEquals("domain", expectedDomainValue, cookie.getDomain());
        assertEquals("name", "hello", cookie.getName());
        assertEquals("value", "world", cookie.getValue());
    }
}