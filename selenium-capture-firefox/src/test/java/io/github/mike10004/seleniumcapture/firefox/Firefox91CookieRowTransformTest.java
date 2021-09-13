package io.github.mike10004.seleniumcapture.firefox;

import io.github.mike10004.seleniumcapture.DeserializableCookie;
import io.github.mike10004.seleniumcapture.StandardCookieExploder;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.Assert.*;

public class Firefox91CookieRowTransformTest {


    @Test
    public void transform_retainHttpOnly() throws Exception {
        Firefox91CookieRowTransform t = new Firefox91CookieRowTransform();
        Instant expiry = Instant.now().plus(Duration.ofHours(24));
        DeserializableCookie c = DeserializableCookie.builder("foo", "bar")
                        .expiry(expiry)
                .attribute("baz", "gaw")
                .httpOnly(true)
                .build();
        Map<String, String> row = t.apply(c);
        assertEquals("isHttpOnly", "1", row.get("isHttpOnly"));
    }
}