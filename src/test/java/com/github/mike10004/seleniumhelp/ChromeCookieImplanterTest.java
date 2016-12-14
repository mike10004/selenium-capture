package com.github.mike10004.seleniumhelp;

import com.github.mike10004.seleniumhelp.ChromeWebDriverFactory.CookieImplanter;
import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ChromeCookieImplanterTest {

    @Rule
    public TemporaryFolder tmp = new org.junit.rules.TemporaryFolder();

    @Test
    public void instantiate() {
        DeserializableCookie c = CookieUsageTestBase.newCookie("foo", "bar");
        CookieImplanter instance = new ChromeWebDriverFactory.CookieImplanter(tmp.getRoot().toPath(), () -> ImmutableList.of(c));
        System.out.format("instance: " + instance);
    }
}
