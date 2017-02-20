package com.github.mike10004.seleniumhelp;

import com.github.mike10004.chromecookieimplant.ChromeCookie;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertEquals;

public class ChromeCookieTransformTest {

    @Test
    public void transform() throws Exception {
        ChromeCookieTransform t = new ChromeCookieTransform();
        DeserializableCookie.Builder input = DeserializableCookie.builder("foo", "bar");

        input.setDomain(".example.com");
        input.httpOnly(false);
        input.setSecure(true);
        input.setPath("/");
        ChromeCookie output = t.transform(input.build());
        assertEquals("chrome cookie URL should be https if input cookie is secure", "https", new URL(output.url).getProtocol());
    }
}