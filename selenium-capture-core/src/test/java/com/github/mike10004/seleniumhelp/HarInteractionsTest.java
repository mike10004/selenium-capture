package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.net.MediaType;
import org.junit.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class HarInteractionsTest {

    @Test
    public void parseUri() throws Exception {

        URI simple = URI.create("https://example.com/some/where?foo=bar#heading");
        System.out.format("%s -> %s%n", simple, toString(simple));

        String url = "https://dt.adsafeprotected.com:8443/dt?x=y&tv={c:KABOOM}";
        URI uri = HarInteractions.parseUri(url);
        System.out.format("%s%n%s%n", url, uri);
        assertEquals("https", uri.getScheme());
        System.out.println(toString(uri));
        assertEquals("scheme", "https", uri.getScheme());
        assertEquals("query", "x=y&tv={c:KABOOM}", uri.getQuery());
        assertEquals("host", "dt.adsafeprotected.com", uri.getHost());
        assertEquals("port", 8443, uri.getPort());
        assertEquals("userInfo", null, uri.getUserInfo());
    }

    private static String toString(URI uri) {
        return MoreObjects.toStringHelper(uri)
                .add("scheme", uri.getScheme())
                .add("host", uri.getHost())
                .add("port", uri.getPort())
                .add("path", uri.getPath())
                .add("query", uri.getQuery())
                .add("ssp", uri.getSchemeSpecificPart())
                .add("authority", uri.getAuthority())
                .add("userInfo", uri.getUserInfo())
                .add("fragment", uri.getFragment())
                .toString();
    }

    @Test
    public void maybeGetCharset() {
        assertEquals(Optional.of(UTF_8), HarInteractions.maybeGetCharset(MediaType.JSON_UTF_8.toString()));
        assertEquals(Optional.empty(), HarInteractions.maybeGetCharset(null));
        assertEquals(Optional.of(ISO_8859_1), HarInteractions.maybeGetCharset("text/html; charset=iso-8859-1"));
        assertEquals(Optional.empty(), HarInteractions.maybeGetCharset("image/jpeg"));
    }
}