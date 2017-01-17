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

    @Test
    public void transform_deserializable() throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String dcJson = "{\"name\":\"JSESSIONID\",\"attribs\":{\"path\":\"/\",\"domain\":\".www.linkedin.com\",\"max-age\":\"7776000\",\"version\":\"1\"},\"value\":\"ajax:6662599637803940570\",\"cookieDomain\":\"www.linkedin.com\",\"cookieExpiryDate\":\"Apr 16, 2017 5:52:11 PM\",\"cookiePath\":\"/\",\"isSecure\":true,\"cookieVersion\":0,\"creationDate\":\"Jan 16, 2017 4:51:57 PM\",\"httpOnly\":false}";
        DeserializableCookie d = gson.fromJson(dcJson, DeserializableCookie.class);
        ChromeCookieTransform t = new ChromeCookieTransform();
        ChromeCookie c = t.transform(d);
        gson.toJson(c, System.out);
        System.out.println();

        DeserializableCookie[] cookies;
        try (java.io.Reader reader = new java.io.FileReader("/tmp/cookies.json")) {
            cookies = gson.fromJson(reader, DeserializableCookie[].class);
        }
        List<ChromeCookie> ccookies = Stream.of(cookies).map(t::transform).collect(Collectors.toList());
        File outfile = new File("/tmp/chrome-cookies.json");
        try (java.io.Writer writer = new java.io.FileWriter(outfile)) {
            gson.toJson(ccookies, writer);
        }
        System.out.format("%s written with ChromeCookie array%n", outfile);
    }

}