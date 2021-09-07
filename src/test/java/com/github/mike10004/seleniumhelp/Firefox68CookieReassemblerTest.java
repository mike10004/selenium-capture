package com.github.mike10004.seleniumhelp;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Firefox68CookieReassemblerTest {

    @Test
    public void reassemble() throws Exception {
        FirefoxCookieReassembler c = new Firefox68CookieReassembler();
        Map<String, Object> mapRep = ExampleCookieSource.asExplodedCookie();
        DeserializableCookie cookie = c.reassemble(mapRep);
        assertNotNull("cookie", cookie);
        assertEquals("name", ExampleCookieSource.name, cookie.getName());
        assertEquals("domain", ExampleCookieSource.originHost, cookie.getDomain());
        assertEquals("attribs", ExampleCookieSource.attribs, cookie.copyAttributes()); // not sure how to test this
        assertEquals("expiry", UnitTests.truncateToSeconds(ExampleCookieSource.expiryDateMillisSinceEpoch), UnitTests.truncateToSeconds(cookie.getExpiryInstant().toEpochMilli()));
        assertEquals("created", UnitTests.truncateToSeconds(ExampleCookieSource.createdDateMillisSinceEpoch), UnitTests.truncateToSeconds(cookie.getCreationInstant().toEpochMilli()));
        assertEquals("accessed", UnitTests.truncateToSeconds(ExampleCookieSource.accessDateMillisSinceEpoch), UnitTests.truncateToSeconds(cookie.getLastAccessedInstant().toEpochMilli()));
        assertEquals("path", ExampleCookieSource.path, cookie.getPath());
        assertEquals("comment", null, cookie.getComment());
        assertEquals("value", ExampleCookieSource.value, cookie.getValue());
        assertEquals("secure", ExampleCookieSource.secure, cookie.isSecure());
        assertEquals("httpOnly", ExampleCookieSource.httpOnly, cookie.isHttpOnly());
    }
}
