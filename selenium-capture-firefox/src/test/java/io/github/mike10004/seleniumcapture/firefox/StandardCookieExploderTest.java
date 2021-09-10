package io.github.mike10004.seleniumcapture.firefox;

import io.github.mike10004.seleniumcapture.DeserializableCookie;
import io.github.mike10004.seleniumcapture.ExplodedCookieConverter;
import io.github.mike10004.seleniumcapture.StandardCookieExploder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.mike10004.seleniumcapture.testing.MapMatcher;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class StandardCookieExploderTest {

    @Test
    public void explode() throws Exception {
        ExplodedCookieConverter conv = new StandardCookieExploder();
        DeserializableCookie c = ExampleCookieSource.asDeserializableCookie();
        Map<String, Object> exploded = conv.explode(c);
        assertNotNull(exploded);
        assertEquals("should not have any Date values", ImmutableSet.of(), exploded.entrySet().stream().filter(entry -> entry.getValue() instanceof Date).map(Map.Entry::getKey).collect(Collectors.toSet()));
        ImmutableMap<String, Object> expected = ExampleCookieSource.asExplodedCookie();
        MatcherAssert.assertThat("exploded", exploded, MapMatcher.expectingWithTruncatedMillis(expected));
    }

    @Test
    public void explode_almostEmpty() throws Exception {
        ExplodedCookieConverter conv = new StandardCookieExploder();
        DeserializableCookie cookie = DeserializableCookie.builder("x", null).build();
        Map<String, Object> exploded = conv.explode(cookie);
        assertNotNull(exploded);
        // this is kind of a weird check, but the exploded cookie map does contain one entry (for 'isSecure')
        assertEquals("conversion of empty cookie results in mostly-empty map", ImmutableSet.of(new SimpleImmutableEntry<>("name", "x"), new SimpleImmutableEntry<>("value", "")), exploded.entrySet());
    }

    @Test
    public void explode_cookieMissingSomeStuff() throws Exception {
        ExplodedCookieConverter conv = new StandardCookieExploder();
        DeserializableCookie cookie = DeserializableCookie.builder("foo", "bar").domain(".example.com").path("/").build();
        Map<String, Object> exploded = conv.explode(cookie);
        assertNotNull(exploded);
        // mostly checking that no exception was thrown, but we'll check map size for good measure
        assertTrue("exploded map has several properties", exploded.size() >= 4);
    }

    @Test
    public void explode_dates() throws Exception {
        StandardCookieExploder d2m = new StandardCookieExploder();
        DeserializableCookie d = ExampleCookieSource.asDeserializableCookie();
        Map<String, Object> map = d2m.explode(d);
        assertNotNull(map);
        assertTrue("creationDate is date type", map.get(DeserializableCookie.FIELD_CREATION_DATE) instanceof Instant);
        assertTrue("lastAccessed is date type", map.get(DeserializableCookie.FIELD_LAST_ACCESSED) instanceof Instant);
        assertTrue("expiry is date type", map.get(DeserializableCookie.FIELD_EXPIRY_DATE) instanceof Instant);
    }

    @Test
    public void explode_httpOnly() throws Exception {
        StandardCookieExploder d2m = new StandardCookieExploder();
        DeserializableCookie d = DeserializableCookie.builder("foo", "bar")
                .domain("example.com")
                .httpOnly(true)
                .build();
        Map<String, Object> map = d2m.explode(d);
        assertNotNull(map);
        assertEquals("httpOnly", map.get(DeserializableCookie.FIELD_HTTP_ONLY), Boolean.TRUE);
    }

    @Test
    public void explode_isSecure() throws Exception {
        StandardCookieExploder d2m = new StandardCookieExploder();
        DeserializableCookie d = DeserializableCookie.builder("foo", "bar")
                .domain("example.com")
                .secure(true)
                .build();
        Map<String, Object> map = d2m.explode(d);
        assertNotNull(map);
        assertEquals("isSecure", map.get(DeserializableCookie.FIELD_IS_SECURE), Boolean.TRUE);
    }

    @Test
    public void retainNameAndValue() throws Exception {
        ExplodedCookieConverter explodedCookieConverter = new StandardCookieExploder();
        DeserializableCookie cookie = DeserializableCookie.builder("foo", "bar").domain("example.com").expiry(Instant.now().plus(Duration.ofHours(24))).build();
        Map<String, Object> explosion = explodedCookieConverter.explode(cookie);
        assertNotNull(explosion);
        assertEquals("name in " + explosion, "foo", explosion.get("name"));
        assertEquals("value in " + explosion, "bar", explosion.get("value"));
    }

    @Test
    public void attributesMapIsStringStringMap() {
        DeserializableCookie c = DeserializableCookie.builder("foo", "bar")
                .domain("example.com")
                .attribute("baz", "gaw")
                .build();
        @SuppressWarnings("unchecked")
        Map<String, String> attribs = (Map<String, String>) new StandardCookieExploder().explode(c).get(DeserializableCookie.FIELD_ATTRIBUTES);
        assertEquals("attribs", ImmutableMap.of("baz", "gaw"), attribs);
    }

    @Test
    public void attributesMapNullIfEmpty() {
        DeserializableCookie c = DeserializableCookie.builder("foo", "bar")
                .domain("example.com")
                .build();
        @SuppressWarnings("unchecked")
        Map<String, String> attribs = (Map<String, String>) new StandardCookieExploder().explode(c).get(DeserializableCookie.FIELD_ATTRIBUTES);
        assertNull("attribs", attribs);
    }
}