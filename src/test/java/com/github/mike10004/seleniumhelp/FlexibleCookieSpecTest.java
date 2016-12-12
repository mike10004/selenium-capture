package com.github.mike10004.seleniumhelp;

import com.google.common.net.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.DefaultCookieSpecProvider;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.*;

public class FlexibleCookieSpecTest {

    @Test
    public void parse() throws Exception {
        String googleCookie = "NID=91=oI2ExtvFoN4kD-2WpEnUakCOCF8v7hYBdstNvNjpytHSdD4J0Xe4HhQaCZrEfK_n1fBV4g-XVupMh-rOqu9iEObzBgTaSxThq3XINSgRFMo7a2a3npe9VBn4wBOFJsskdF1eTvYyADyjr3k; expires=Fri, 02-Jun-2017 21:54:46 GMT; path=/; domain=.google.com; HttpOnly";
        FlexibleCookieSpec spec = FlexibleCookieSpec.getDefault();
        CookieOrigin origin = CookieUtility.getInstance().buildCookieOrigin(new java.net.URL("https://www.google.com/")).getLeft();
        List<Cookie> cookies = spec.parse(new BasicHeader(HttpHeaders.SET_COOKIE, googleCookie), origin);
        assertEquals("size", 1, cookies.size());
        Cookie c = cookies.get(0);
        assertEquals("name", "NID", c.getName());
        assertEquals("domain", "www.google.com", c.getDomain());
        assertEquals("domain", ".google.com", ((DeserializableCookie)c).getDomainAttribute());
        assertEquals("path", "/", c.getPath());
    }

    @Test
    public void match_true() throws Exception {
        URL url = new java.net.URL("https://www.google.com/");
        BasicClientCookie cookie = new BasicClientCookie("foo", "bar");
        cookie.setDomain(".google.com");
        testMatch(cookie, url, true);
    }

    @Test
    public void match_false() throws Exception {
        URL url = new java.net.URL("https://www.example.com/");
        BasicClientCookie cookie = new BasicClientCookie("foo", "bar");
        cookie.setDomain(".google.com");
        testMatch(cookie, url, false);
    }

    private void testMatch(Cookie cookie, URL url, boolean expected) throws IOException {
        FlexibleCookieSpec spec = new FlexibleCookieSpec(new DefaultCookieSpecProvider().create(new BasicHttpContext()));
        CookieOrigin origin = CookieUtility.getInstance().buildCookieOrigin(url).getLeft();

        assertEquals(cookie + " matches " + origin, expected, spec.match(cookie, origin));
    }

}