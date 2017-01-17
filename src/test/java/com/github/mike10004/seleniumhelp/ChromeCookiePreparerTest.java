package com.github.mike10004.seleniumhelp;

import com.github.mike10004.chromecookieimplant.ChromeCookie;
import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Date;

import static org.junit.Assert.*;

public class ChromeCookiePreparerTest {

    @Rule
    public TemporaryFolder tmp = new org.junit.rules.TemporaryFolder();

    @Test
    public void instantiate() {
        DeserializableCookie c = CookieUsageTestBase.newCookie("foo", "bar");
        ChromeCookiePreparer instance = new ChromeCookiePreparer(tmp.getRoot().toPath(), () -> ImmutableList.of(c));
        System.out.format("instance: " + instance);
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
