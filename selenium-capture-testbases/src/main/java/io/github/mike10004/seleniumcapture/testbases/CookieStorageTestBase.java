package io.github.mike10004.seleniumcapture.testbases;

import io.github.mike10004.seleniumcapture.DeserializableCookie;
import io.github.mike10004.seleniumcapture.HarAnalysis;
import io.github.mike10004.seleniumcapture.HarPlus;
import io.github.mike10004.seleniumcapture.SetCookieHeaderParser;
import io.github.mike10004.seleniumcapture.TrafficCollector;
import io.github.mike10004.seleniumcapture.WebDriverFactory;
import com.github.mike10004.xvfbtesting.XvfbRule;
import com.browserup.harreader.model.Har;
import io.github.mike10004.seleniumcapture.testing.UnitTests;
import org.apache.http.cookie.MalformedCookieException;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class CookieStorageTestBase {

    @Rule
    public XvfbRule xvfb = UnitTests.xvfbRuleBuilder().build();

    private final WebDriverTestParameter webDriverTestParameter;

    public CookieStorageTestBase(WebDriverTestParameter webDriverTestParameter) {
        this.webDriverTestParameter = webDriverTestParameter;
    }

    @Test
    public void testCookieStorage() throws Exception {
        webDriverTestParameter.doDriverManagerSetup();
        WebDriverFactory factory = webDriverTestParameter.createWebDriverFactory(xvfb);
        testWithDriverFactory(factory);
    }

    private void testWithDriverFactory(WebDriverFactory factory) throws IOException, MalformedCookieException {
        HarPlus<Void> collection = TrafficCollector.builder(factory)
                .collectHttps(TestCertificateAndKeySource.create()).build().collect(driver -> {
            driver.get("https://httprequestecho.appspot.com/cookies/set");
            return (Void) null;
        });
        Har har = collection.har;
        final SetCookieHeaderParser cookieSpec = SetCookieHeaderParser.create();
        List<DeserializableCookie> harCookies = HarAnalysis.of(har).findCookies(cookieSpec).makeUltimateCookieList();
        if (harCookies.size() > 1) {
            System.out.println("multiple cookies found");
            for (DeserializableCookie cookie : harCookies) {
                System.out.format("%s%n", cookie);
            }
        }
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
            assertNotNull("expiry", c.getExpiryInstant());
        });
    }

}
