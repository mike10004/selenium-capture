package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.bonigarcia.wdm.FirefoxDriverManager;
import net.lightbody.bmp.core.har.Har;
import org.apache.http.cookie.MalformedCookieException;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CookieStorageTest {

    @Rule
    public XvfbRule xvfb = XvfbRule.builder().disabledOnWindows().build();

    @Test
    public void testFirefox() throws Exception {
        FirefoxDriverManager.getInstance().setup();
        testWithDriverFactory(new FirefoxWebDriverFactory(xvfb.getController().configureEnvironment(new HashMap<>()),
                ImmutableMap.of(), ImmutableList.of()));
    }

    @Test
    public void testChrome() throws Exception {
        ChromeDriverManager.getInstance().setup();
        testWithDriverFactory(new ChromeWebDriverFactory(xvfb.getController().configureEnvironment(new HashMap<>()), new ChromeOptions(), new DesiredCapabilities()));
    }

    private void testWithDriverFactory(WebDriverFactory factory) throws IOException, MalformedCookieException {
        HarPlus<Void> collection = new HttpsTestTrafficCollector(factory).collect(driver -> {
            driver.get("https://httprequestecho.appspot.com/cookies/set");
            return (Void) null;
        });
        Har har = collection.har;
        final FlexibleCookieSpec cookieSpec = FlexibleCookieSpec.getDefault();
        List<DeserializableCookie> harCookies = HarAnalysis.of(har).findCookies(cookieSpec).makeUltimateCookieList();
        assertEquals("num cookies", 1, harCookies.size());
        checkCookies(harCookies);
    }

    private void checkCookies(Iterable<DeserializableCookie> cookies) {
        cookies.forEach(c -> {
            System.out.format("cookie: %s%n", c);
            assertNotNull("name", c.getName());
            assertNotNull("value", c.getValue());
            assertNotNull("path", c.getPath());
            assertNotNull("domain", c.getDomain());
            assertNotNull("domain attribute", c.getDomainAttribute());
            assertNotNull("expiry", c.getExpiryDate());
        });
    }

}
