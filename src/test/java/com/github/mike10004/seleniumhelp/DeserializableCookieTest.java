package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class DeserializableCookieTest {

    @Test
    public void deserialize() throws Exception {
        String json = "{\"name\": \"foo\", \"value\": \"bar\", \"attribs\": {\"baz\": \"gaw\"}}";
        DeserializableCookie c = new Gson().fromJson(json, DeserializableCookie.class);
        assertEquals("foo", c.getName());
        assertEquals("bar", c.getValue());
        assertEquals("gaw", c.getAttribute("baz"));
    }

    @Test
    public void deserializeDate() throws Exception {
        String expiryStr = "Jun 25, 2019 12:07:27 PM";
        String json = "{\n" +
                "    \"name\": \"myCookie\",\n" +
                "    \"value\": \"blahblahblah\",\n" +
                "    \"attribs\": {\n" +
                "      \"domain\": \"localhost\"\n" +
                "    },\n" +
                "    \"cookieDomain\": \"localhost\",\n" +
                "    \"cookieExpiryDate\": \"" + expiryStr + "\",\n" +
                "    \"cookiePath\": \"/\"\n" +
                "  }";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expectedExpiryDate = sdf.parse(expiryStr);
        Gson gson = new Gson();
        DeserializableCookie cookie = gson.fromJson(json, DeserializableCookie.class);
        assertEquals("expiry upon deserialization", expectedExpiryDate, cookie.getExpiryDate());
        json = gson.toJson(cookie);
        cookie = gson.fromJson(json, DeserializableCookie.class);
        assertEquals("expiry after inversion", expectedExpiryDate, cookie.getExpiryDate());
    }

    @Test
    public void isPersistent() throws Exception {
        Map<DeserializableCookie, Boolean> testCases = ImmutableMap.<DeserializableCookie, Boolean>builder()
                .put(DeserializableCookie.builder("foo", "bar").creationDate(Instant.now()).attribute("max-age", "3600").build(), true)
                .put(DeserializableCookie.builder("foo", "bar").creationDate(Instant.now()).attribute("max-age", "3600").expiry(Instant.now().plusSeconds(120)).build(), true)
                .put(DeserializableCookie.builder("foo", "bar").expiry(Instant.now().plus(Duration.ofDays(3))).build(), true)
                .put(DeserializableCookie.builder("foo", "bar").build(), false)
                .build();
        testCases.forEach((cookie, expected) -> {
            boolean actual = cookie.isPersistent();
            assertEquals(cookie.toString(), expected.booleanValue(), actual);
        });
    }

    @Test
    public void getBestExpiry() throws Exception {
        long maxAge = 3600;
        Instant now = Instant.now();
        DeserializableCookie c = DeserializableCookie.builder("foo", "bar")
                .attribute("max-age", String.valueOf(maxAge))
                .creationDate(now)
                .build();
        Instant bestExpiry = c.getBestExpiry();
        System.out.format("best expiry: %s for %s%n", c.getBestExpiry(), c);
        assertNotNull("bestExpiry", bestExpiry);
    }

    @Test
    public void expiryOveflow() throws Exception {
        long maxAge = Long.MAX_VALUE;
        Instant now = Instant.now();
        DeserializableCookie c = DeserializableCookie.builder("foo", "bar")
                .attribute("max-age", String.valueOf(maxAge))
                .creationDate(now)
                .build();
        Instant bestExpiry = c.getBestExpiry();
        System.out.format("best expiry: %s for %s%n", bestExpiry, c);
        assertNotNull("getBestExpiry result", bestExpiry);
        assertFalse("expired", c.isExpired(Instant.now()));
    }
}