package com.github.mike10004.seleniumhelp;

import com.google.common.net.HttpHeaders;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.cookie.DefaultCookieSpecProvider;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.Test;

import java.net.URI;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for cookie converters. We can't test that there is perfect lossless
 * conversion of cookies, because there isn't. Some cookie classes have
 * some fields that others don't. But we have these tests that confirm that
 * the universal fields (those captured by {@link org.openqa.selenium.Cookie})
 * are retained through conversions and that no exceptions are thrown.
 */
public class SeleniumCookieConverterTest {

    @Test
    public void doForward_httpOnlyTrue() throws Exception {
        testDoForward(true);
    }

    @Test
    public void doForward_httpOnlyFalse() throws Exception {
        testDoForward(false);
    }

    private void testDoForward(boolean httpOnly) throws Exception {
        String exampleSetCookieHeaderValue = "YP=v=AwAAY&d=AEgAMEUCIAphCQ1YdKiZJVYwQOKscLhHZEHsT5JhZcSiQ.Hi5AjKAiEAhqK8LNfPJpAyDnvLzNZV_ByH9Zjz3v55lqYO2eiL4msA; expires=Thu, 29-Nov-2018 19:22:24 GMT; path=/; domain=.yahoo.com; secure; HttpOnly";
        org.apache.http.cookie.Cookie reference = parseCookie(URI.create("https://yahoo.com:443/some/where"), exampleSetCookieHeaderValue);
        org.openqa.selenium.Cookie input = toSeleniumCookie(reference, httpOnly);
        DeserializableCookie output = new SeleniumCookieConverter().doForward(input);
        assertEquals("cookie equals", true, isEqualEnough(output, input));
    }

    private org.openqa.selenium.Cookie toSeleniumCookie(org.apache.http.cookie.Cookie reference, boolean httpOnly) {
        org.openqa.selenium.Cookie c = new org.openqa.selenium.Cookie(reference.getName(), reference.getValue(), reference.getDomain(), reference.getPath(), reference.getExpiryDate(), reference.isSecure(), httpOnly);
        return c;
    }

    @Test
    public void doBackward() throws Exception {
        String exampleSetCookieHeaderValue = "YP=v=AwAAY&d=AEgAMEUCIAphCQ1YdKiZJVYwQOKscLhHZEHsT5JhZcSiQ.Hi5AjKAiEAhqK8LNfPJpAyDnvLzNZV_ByH9Zjz3v55lqYO2eiL4msA; expires=Thu, 29-Nov-2018 19:22:24 GMT; path=/; domain=.yahoo.com; secure; HttpOnly";
        org.apache.http.cookie.Cookie reference = parseCookie(URI.create("https://yahoo.com:443/some/where"), exampleSetCookieHeaderValue);
        DeserializableCookie input = toDeserializableCookie(reference);
        org.openqa.selenium.Cookie output = new SeleniumCookieConverter().doBackward(input);
        assertEquals("cookie equals", true, isEqualEnough(input, output));
    }

    private static boolean isEqualEnough(DeserializableCookie a, org.openqa.selenium.Cookie b) {
        EqualsBuilder eq = new EqualsBuilder()
                .append(b.getDomain(), a.getDomain())
                .append(b.getExpiry(), a.getExpiryDate())
                .append(b.getName(), a.getName())
                .append(b.getPath(), a.getPath())
                .append(b.getValue(), a.getValue())
                .append(b.isSecure(), a.isSecure())
                .append(b.isHttpOnly(), a.isHttpOnly());
        return eq.isEquals();
    }

    private static DeserializableCookie toDeserializableCookie(org.apache.http.cookie.Cookie reference) {
        DeserializableCookie.Builder c = DeserializableCookie.builder(reference.getName(), reference.getValue());
        c.setComment(reference.getComment());
        c.setDomain(reference.getDomain());
        c.setExpiryDate(reference.getExpiryDate());
        c.setPath(reference.getPath());
        c.setSecure(reference.isSecure());
        return c.build();
    }

    private static CookieOrigin urlToOrigin(URI uri) {
        CookieOrigin origin = new CookieOrigin(uri.getHost(), uri.getPort(), uri.getPath(), "https".equals(uri.getScheme()));
        return origin;
    }

    private static org.apache.http.cookie.Cookie parseCookie(URI cookieOrigin, String setCookieHeaderValue) throws MalformedCookieException {
        return parseCookie(urlToOrigin(cookieOrigin), setCookieHeaderValue);
    }

    private static org.apache.http.cookie.Cookie parseCookie(CookieOrigin origin, String setCookieHeaderValue) throws MalformedCookieException {
        CookieSpec cookieSpec = new DefaultCookieSpecProvider().create(new BasicHttpContext());
        List<org.apache.http.cookie.Cookie> cookies = cookieSpec.parse(new BasicHeader(HttpHeaders.SET_COOKIE, setCookieHeaderValue), origin);
        checkState(cookies.size() == 1, "wrong number of cookies in header: %s", cookies.size());
        return cookies.get(0);
    }

}