package com.github.mike10004.seleniumhelp;

import com.github.mike10004.chromecookieimplant.ChromeCookie;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class ChromeCookieTransformTest {

    @Test
    public void transform() throws Exception {
        ChromeCookieTransform t = new ChromeCookieTransform();
        DeserializableCookie input = new DeserializableCookie();
        input.setDomain(".example.com");
        input.setHttpOnly(false);
        input.setSecure(true);
        input.setPath("/");
        ChromeCookie output = t.transform(input);
        assertEquals("chrome cookie URL should be https if input cookie is secure", "https", new URL(output.url).getProtocol());
    }
}