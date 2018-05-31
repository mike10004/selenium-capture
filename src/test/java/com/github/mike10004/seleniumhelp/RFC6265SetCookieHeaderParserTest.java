package com.github.mike10004.seleniumhelp;

import com.google.common.net.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.message.BasicHeader;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class RFC6265SetCookieHeaderParserTest {

    @Test
    public void parse() throws Exception {
        String googleCookie = "NID=91=oI2ExtvFoN4kD-2WpEnUakCOCF8v7hYBdstNvNjpytHSdD4J0Xe4HhQaCZrEfK_n1fBV4g-XVupMh-rOqu9iEObzBgTaSxThq3XINSgRFMo7a2a3npe9VBn4wBOFJsskdF1eTvYyADyjr3k; expires=Fri, 02-Jun-2017 21:54:46 GMT; path=/; domain=.google.com; HttpOnly";
        RFC6265SetCookieHeaderParser parser = new RFC6265SetCookieHeaderParser(CookieAttributeHandlers.getDefaultAttributeHandlers());
        CookieOrigin origin = CookieUtility.getInstance().buildCookieOrigin(new java.net.URL("https://www.google.com/")).getLeft();
        List<Cookie> cookies = parser.parse(new BasicHeader(HttpHeaders.SET_COOKIE, googleCookie), origin, null);
        assertEquals("size", 1, cookies.size());
        DeserializableCookie c = (DeserializableCookie) cookies.get(0);
        System.out.format("%s%n", c);
        assertEquals("name", "NID", c.getName());
        assertEquals("domain", "www.google.com", c.getDomain());
        assertEquals("domain", ".google.com", c.getDomainAttribute());
        assertEquals("path", "/", c.getPath());
        assertNotNull("expiry null", c.getExpiryDate());
    }

}