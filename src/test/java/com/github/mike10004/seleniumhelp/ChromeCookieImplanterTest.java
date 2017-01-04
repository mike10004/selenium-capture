package com.github.mike10004.seleniumhelp;

import com.github.mike10004.seleniumhelp.ChromeWebDriverFactory.ChromeCookieTransform;
import com.github.mike10004.seleniumhelp.ChromeWebDriverFactory.CookieImplanter;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class ChromeCookieImplanterTest {

    @Rule
    public TemporaryFolder tmp = new org.junit.rules.TemporaryFolder();

    @Test
    public void instantiate() {
        DeserializableCookie c = CookieUsageTestBase.newCookie("foo", "bar");
        CookieImplanter instance = new ChromeWebDriverFactory.CookieImplanter(tmp.getRoot().toPath(), () -> ImmutableList.of(c));
        System.out.format("instance: " + instance);
    }

    @Test
    public void serialize() {
        DeserializableCookie c = CookieUsageTestBase.newCookie("foo", "bar");
        CookieImplanter instance = new ChromeWebDriverFactory.CookieImplanter(tmp.getRoot().toPath(), () -> ImmutableList.of(c));
        URI uri = instance.buildImplantUriFromCookies(Stream.of(c));
        List<String> queryParts = Splitter.on('=').limit(2).splitToList(uri.getQuery());
        checkState("import".equals(queryParts.get(0)));
        String cookieJson = queryParts.get(1);
        JsonObject cookieObject = new JsonParser().parse(cookieJson).getAsJsonObject();
        assertEquals("name", "foo", cookieObject.get("name").getAsString());
        assertEquals("value", "bar", cookieObject.get("value").getAsString());
        assertFalse("cookie has 'session' field but shouldn't", cookieObject.has("session"));
    }

    @Test
    public void ChromeCookieTransform_transform_noExpiryDate() {
        DeserializableCookie d = new DeserializableCookie();
        d.setName("foo");
        d.setValue("bar");
        d.setDomain("example.com");
        ChromeCookieTransform transform = new ChromeCookieTransform();
        ChromeCookie c = transform.transform(d);
        assertEquals("name", d.getName(), c.name);
        assertEquals("value", d.getValue(), c.value);
        assertEquals("domain", d.getBestDomainProperty(), c.domain);
        assertNull("expirationDate", c.expirationDate);
    }

    @Test
    public void ChromeCookieTransform_transform() {
        DeserializableCookie d = new DeserializableCookie();
        d.setName("foo");
        d.setValue("bar");
        d.setDomain("example.com");
        Date expiryDate = new Date();
        d.setExpiryDate(expiryDate);
        ChromeCookieTransform transform = new ChromeCookieTransform();
        ChromeCookie c = transform.transform(d);
        assertEquals("name", d.getName(), c.name);
        assertEquals("value", d.getValue(), c.value);
        assertEquals("domain", d.getBestDomainProperty(), c.domain);
        double expiryDateInSeconds = expiryDate.getTime() / 1000d;
        assertEquals("expirationDate", expiryDateInSeconds, c.expirationDate.doubleValue(), 0.0001);
    }
}
