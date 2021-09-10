package io.github.mike10004.seleniumcapture.chrome;

import com.github.mike10004.chromecookieimplant.ChromeCookie;
import com.github.mike10004.seleniumhelp.DeserializableCookie;
import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ChromeCookiePreparerTest {

    @Rule
    public TemporaryFolder tmp = new org.junit.rules.TemporaryFolder();

    @Test
    public void instantiate() {
        DeserializableCookie c = newCookie("foo", "bar");
        ChromeCookiePreparer instance = new ChromeCookiePreparer(tmp.getRoot().toPath(), () -> ImmutableList.of(c));
        System.out.format("instance: " + instance);
    }

    @Test
    public void ChromeCookieTransform_transform_noExpiryDate() {
        DeserializableCookie d = DeserializableCookie.builder("foo", "bar").domain("example.com").build();
        ChromeCookieTransform transform = new ChromeCookieTransform();
        ChromeCookie c = transform.transform(d);
        assertEquals("name", d.getName(), c.name);
        assertEquals("value", d.getValue(), c.value);
        assertEquals("domain", d.getBestDomainProperty(), c.domain);
        assertNull("expirationDate", c.expirationDate);
    }

    @Test
    public void ChromeCookieTransform_transform() {
        Instant expiryDate = Instant.now();
        DeserializableCookie d = DeserializableCookie.builder("foo", "bar").domain("example.com").expiry(expiryDate).build();
        ChromeCookieTransform transform = new ChromeCookieTransform();
        ChromeCookie c = transform.transform(d);
        assertEquals("name", d.getName(), c.name);
        assertEquals("value", d.getValue(), c.value);
        assertEquals("domain", d.getBestDomainProperty(), c.domain);
        double expiryDateInSeconds = expiryDate.toEpochMilli() / 1000d;
        assertEquals("expirationDate", expiryDateInSeconds, c.expirationDate.doubleValue(), 0.0001);
    }

    @SuppressWarnings("SameParameterValue")
    public static DeserializableCookie newCookie(String name, String value) {
        DeserializableCookie.Builder cookie = DeserializableCookie.builder(name, value);
        cookie.httpOnly(true);
        Instant now = Instant.now();
        Instant later = now.plus(Duration.ofDays(90));
        cookie.creationDate(now);
        cookie.lastAccessed(now);
        cookie.expiry(later);
        cookie.setSecure(true);
        cookie.setDomain("localhost");
        cookie.setPath("/");
        return cookie.build();
    }


}
