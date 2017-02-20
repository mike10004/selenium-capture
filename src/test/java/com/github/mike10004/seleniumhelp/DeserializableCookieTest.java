package com.github.mike10004.seleniumhelp;

import com.google.gson.Gson;
import org.junit.Test;

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

}