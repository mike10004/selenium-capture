package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.BiConsumer;

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

    private static Iterator<Instant> instants(Instant start, Duration increment) {
        return new Iterator<Instant>() {

            private Instant previous = start;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public synchronized Instant next() {
                Instant next = previous.plus(increment);
                previous = next;
                return next;
            }
        };
    }

    @Test
    public void serializationOfDateFields_all() throws Exception {
        Iterator<Instant> instants = instants(Instant.now().minus(Duration.ofDays(7)), Duration.ofHours(1));
        DeserializableCookie.Builder cookieBuilder = DeserializableCookie.builder("foo", "bar");
        dateFieldSetters().forEach(setter -> setter.accept(cookieBuilder, instants.next()));
        DeserializableCookie cookie = cookieBuilder.build();
        DeserializableCookie deserialized = invertTwice(cookie);
        assertEquals("deserialized", cookie, deserialized);
    }

    @Test
    public void serializationOfDateFields_individual() throws Exception {
        Iterator<Instant> instants = instants(Instant.now().minus(Duration.ofDays(7)), Duration.ofHours(1));
        dateFieldSetters().forEach(setter -> {
            DeserializableCookie.Builder cookieBuilder = DeserializableCookie.builder("foo", "bar");
            setter.accept(cookieBuilder, instants.next());
            DeserializableCookie cookie = cookieBuilder.build();
            DeserializableCookie deserialized = invertTwice(cookie);
            assertEquals("deserialized", cookie, deserialized);
        });
    }

    private static Iterable<BiConsumer<? super DeserializableCookie.Builder, Instant>> dateFieldSetters() {
        return Arrays.asList(
                DeserializableCookie.Builder::creationDate,
                DeserializableCookie.Builder::lastAccessed,
                DeserializableCookie.Builder::expiry
        );
    }

    private static DeserializableCookie invertTwice(DeserializableCookie cookie) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(cookie);
        System.out.println(json);
        DeserializableCookie deserialized = gson.fromJson(json, DeserializableCookie.class);
        return deserialized;
    }

    @Test
    public void deserialize_instantsAsStrings() throws Exception {
        String json = "{\n" +
                "      \"name\": \"foo\",\n" +
                "      \"value\": \"bar\",\n" +
                "      \"cookiePath\": \"/\",\n" +
                "      \"cookieDomain\": \".example.com\",\n" +
                "      \"attribs\": {},\n" +
                "      \"cookieExpiryDate\": \"2018-03-11T05:18:29Z\",\n" +
                "      \"creationDate\": \"2016-03-10T17:40:57.274Z\",\n" +
                "      \"lastAccessed\": \"2016-03-10T17:42:21.853Z\",\n" +
                "      \"isSecure\": false\n" +
                "    }";
        DeserializableCookie cookie = new Gson().fromJson(json, DeserializableCookie.class);
        assertNotNull("lastAccessed", cookie.getLastAccessedInstant());
    }
}