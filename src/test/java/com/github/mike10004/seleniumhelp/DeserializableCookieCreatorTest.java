package com.github.mike10004.seleniumhelp;

import com.google.common.collect.Iterables;
import com.google.common.net.HttpHeaders;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.impl.cookie.DefaultCookieSpecProvider;
import org.apache.http.impl.cookie.NetscapeDraftSpec;
import org.apache.http.impl.cookie.RFC6265LaxSpec;
import org.apache.http.impl.cookie.RFC6265StrictSpec;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.Test;

import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;

@org.junit.Ignore
public class DeserializableCookieCreatorTest {


    @Test
    public void testApacheParsing() throws Exception {
        CookieSpec spec = new DefaultCookieSpecProvider().create(new BasicHttpContext());
        testApacheParsing(spec);
    }

    @Test
    public void testApacheParsing_BrowserCompatSpec() throws Exception {
        @SuppressWarnings("deprecation")
        CookieSpec spec = new org.apache.http.impl.cookie.BrowserCompatSpec();
        testApacheParsing(spec);
    }

    @Test
    public void testApacheParsing_BestMatchSpec() throws Exception {
        @SuppressWarnings("deprecation")
        CookieSpec spec = new org.apache.http.impl.cookie.BestMatchSpec();
        testApacheParsing(spec);
    }

    @Test
    public void testApacheParsing_NetscapeDraftSpec() throws Exception {
        CookieSpec spec = new NetscapeDraftSpec();
        testApacheParsing(spec);
    }

    @Test
    public void testApacheParsing_RFC6265LaxSpec() throws Exception {
        CookieSpec spec = new RFC6265LaxSpec();
        testApacheParsing(spec);
    }

    @Test
    public void testApacheParsing_RFC6265StrictSpec() throws Exception {
        CookieSpec spec = new RFC6265StrictSpec();
        testApacheParsing(spec);
    }

    private void testApacheParsing(CookieSpec spec) throws Exception {
        CookieOrigin origin = CookieUtility.getInstance().buildCookieOrigin(new URL("https://www.example.com")).origin;
        List<org.apache.http.cookie.Cookie> cookies = spec.parse(new BasicHeader(HttpHeaders.SET_COOKIE, "hello=world; Path=/; Domain=.example.com"), origin);
        org.apache.http.cookie.Cookie cookie = Iterables.getOnlyElement(cookies);
        assertEquals("domain", ".example.com", cookie.getDomain());
    }
}