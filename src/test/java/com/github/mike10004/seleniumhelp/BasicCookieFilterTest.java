package com.github.mike10004.seleniumhelp;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.junit.Test;
import org.openqa.selenium.Cookie;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;

public class BasicCookieFilterTest {

    @Test
    public void goodWithPublicDomain() throws Exception {
        Date now = Calendar.getInstance().getTime();
        Date later = DateUtils.addMonths(now, 6);
        Cookie goodWithPublicDomain = new Cookie("foo", "123", ".example.com", "/", later, true);
        testPredicate(now, goodWithPublicDomain, true);
    }

    @Test
    public void goodWithSubdomain() throws Exception {
        Date now = Calendar.getInstance().getTime();
        Date later = DateUtils.addMonths(now, 6);
        Cookie goodWithSubdomain = new Cookie("bar", "456", "www.example.com", "/", later, true);
        testPredicate(now, goodWithSubdomain, true);
    }

    @Test
    public void badBecauseWrongDomain1() throws Exception {
        Date now = Calendar.getInstance().getTime();
        Date later = DateUtils.addMonths(now, 6);
        Cookie badBecauseWrongDomain = new Cookie("baz", "789", ".google.com", "/", later, true);
        testPredicate(now, badBecauseWrongDomain, false);
    }

    @Test
    public void badBecauseWrongDomain2() throws Exception {
        Date now = Calendar.getInstance().getTime();
        Date later = DateUtils.addMonths(now, 6);
        Cookie badBecauseWrongDomain = new Cookie("baz", "789", "example.com", "/", later, true);
        testPredicate(now, badBecauseWrongDomain, false, new URL("https://anotherexample.com/"));
    }

    @Test
    public void badBecauseWrongDomain3() throws Exception {
        Date now = Calendar.getInstance().getTime();
        Date later = DateUtils.addMonths(now, 6);
        Cookie badBecauseWrongDomain = new Cookie("baz", "789", "gopher.example.com", "/", later, true);
        testPredicate(now, badBecauseWrongDomain, false);
    }

    @Test
    public void badBecauseExpired() throws Exception {
        Date now = Calendar.getInstance().getTime();
        Date earlier = DateUtils.addDays(now, -7);
        Cookie badBecauseExpired = new Cookie("bar", "456", "www.example.com", "/", earlier, true);
        testPredicate(now, badBecauseExpired, false);
    }

    private void testPredicate(Date now, Cookie cookie, boolean expected) throws MalformedURLException {
        URL url = new URL("https://www.example.com/");
        testPredicate(now, cookie, expected, url);
    }

    private void testPredicate(Date now, Cookie cookie, boolean expected, URL url)  {
        CookieFilter filter = new BasicCookieFilter();
        Predicate<Cookie> predicate = filter.makeSeleniumPredicate(url, now);
        assertEquals("cookie match", expected, predicate.test(cookie));
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

    private void testMatch(org.apache.http.cookie.Cookie cookie, URL url, boolean expected) {
        CookieFilter filter = new BasicCookieFilter();
        CookieOrigin origin = CookieUtility.getInstance().buildCookieOrigin(url).getLeft();
        assertEquals(cookie + " matches " + origin, expected, filter.makeApacheOriginPredicate(origin).test(cookie));
    }

}