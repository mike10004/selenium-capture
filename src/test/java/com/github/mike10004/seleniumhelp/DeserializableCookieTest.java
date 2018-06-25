package com.github.mike10004.seleniumhelp;

import com.google.gson.Gson;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

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
        Date expectedExpiryDate = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a").parse(expiryStr);
        Gson gson = new Gson();
        DeserializableCookie cookie = gson.fromJson(json, DeserializableCookie.class);
        assertEquals("expiry upon deserialization", expectedExpiryDate, cookie.getExpiryDate());
        json = gson.toJson(cookie);
        cookie = gson.fromJson(json, DeserializableCookie.class);
        assertEquals("expiry after inversion", expectedExpiryDate, cookie.getExpiryDate());
    }
}