package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class Firefox68ExplodedCookieConverterTest {

    private static Date truncateToSeconds(long millisSinceEpoch) {
        return DateUtils.truncate(new Date(millisSinceEpoch), Calendar.SECOND);
    }

    @Test
    public void doForward() throws Exception {
        ExplodedCookieConverter c = new Firefox68ExplodedCookieConverter();
        Map<String, Object> mapRep = ExampleCookieSource.asExplodedCookie();
        DeserializableCookie cookie = c.reassemble(mapRep);
        assertNotNull("cookie", cookie);
        assertEquals("name", ExampleCookieSource.name, cookie.getName());
        assertEquals("domain", ExampleCookieSource.originHost, cookie.getDomain());
        assertEquals("attribs", ExampleCookieSource.attribs, cookie.copyAttributes()); // not sure how to test this
        assertEquals("expiry", truncateToSeconds(ExampleCookieSource.expiryDateMillisSinceEpoch), truncateToSeconds(cookie.getExpiryInstant().toEpochMilli()));
        assertEquals("created", truncateToSeconds(ExampleCookieSource.createdDateMillisSinceEpoch), truncateToSeconds(cookie.getCreationInstant().toEpochMilli()));
        assertEquals("accessed", truncateToSeconds(ExampleCookieSource.accessDateMillisSinceEpoch), truncateToSeconds(cookie.getLastAccessedInstant().toEpochMilli()));
        assertEquals("path", ExampleCookieSource.path, cookie.getPath());
        assertEquals("comment", null, cookie.getComment());
        assertEquals("value", ExampleCookieSource.value, cookie.getValue());
        assertEquals("secure", ExampleCookieSource.secure, cookie.isSecure());
        assertEquals("httpOnly", ExampleCookieSource.httpOnly, cookie.isHttpOnly());
    }

    @Test
    public void doBackward() throws Exception {
        ExplodedCookieConverter conv = new Firefox68ExplodedCookieConverter();
        DeserializableCookie c = ExampleCookieSource.asDeserializableCookie();
        Map<String, Object> exploded = conv.explode(c);
        assertNotNull(exploded);
        assertEquals("should not have any Date values", ImmutableSet.of(), exploded.entrySet().stream().filter(entry -> entry.getValue() instanceof Date).map(Map.Entry::getKey).collect(Collectors.toSet()));
        ImmutableMap<String, Object> expected = ExampleCookieSource.asExplodedCookie();
        assertThat("exploded", exploded, MapMatcher.expectingWithTruncatedMillis(expected));
    }

    @Test
    public void doBackward_almostEmpty() throws Exception {
        ExplodedCookieConverter conv = new Firefox68ExplodedCookieConverter();
        DeserializableCookie cookie = DeserializableCookie.builder("x", null).build();
        Map<String, Object> exploded = conv.explode(cookie);
        assertNotNull(exploded);
        // this is kind of a weird check, but the exploded cookie map does contain one entry (for 'isSecure')
        assertEquals("conversion of empty cookie results in mostly-empty map", ImmutableSet.of(new SimpleImmutableEntry<>("name", "x"), new SimpleImmutableEntry<>("value", "")), exploded.entrySet());
    }

    @Test
    public void doBackward_cookieMissingSomeStuff() throws Exception {
        ExplodedCookieConverter conv = new Firefox68ExplodedCookieConverter();
        DeserializableCookie cookie = DeserializableCookie.builder("foo", "bar").domain(".example.com").path("/").build();
        Map<String, Object> exploded = conv.explode(cookie);
        assertNotNull(exploded);
        // mostly checking that no exception was thrown, but we'll check map size for good measure
        assertTrue("exploded map has several properties", exploded.size() >= 4);
    }

    @Test
    public void doBackward_dates() throws Exception {
        Firefox68ExplodedCookieConverter d2m = new Firefox68ExplodedCookieConverter();
        DeserializableCookie d = ExampleCookieSource.asDeserializableCookie();
        Map<String, Object> map = d2m.explode(d);
        assertNotNull(map);
        assertTrue("creationDate is date type", map.get(DeserializableCookie.FIELD_CREATION_DATE) instanceof Instant);
        assertTrue("lastAccessed is date type", map.get(DeserializableCookie.FIELD_LAST_ACCESSED) instanceof Instant);
        assertTrue("expiry is date type", map.get(DeserializableCookie.FIELD_EXPIRY_DATE) instanceof Instant);
    }

    @Test
    public void retainNameAndValue() throws Exception {
        ExplodedCookieConverter explodedCookieConverter = new Firefox68ExplodedCookieConverter();
        DeserializableCookie cookie = DeserializableCookie.builder("foo", "bar").domain("example.com").expiry(Instant.now().plus(Duration.ofHours(24))).build();
        Map<String, Object> explosion = explodedCookieConverter.explode(cookie);
        assertNotNull(explosion);
        assertEquals("name in " + explosion, "foo", explosion.get("name"));
        assertEquals("value in " + explosion, "bar", explosion.get("value"));
    }
}