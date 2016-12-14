package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbmanager.XvfbController;
import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.net.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Rule;
import org.openqa.selenium.WebDriverException;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public abstract class CookieUsageTestBase {

    @Rule
    public XvfbRule xvfb = XvfbRule.builder().disabledOnWindows().build();

    protected abstract WebDriverFactory createCookielessWebDriverFactory(XvfbController xvfbController);

    private List<DeserializableCookie> browseAndSetCookies(URL url, XvfbController xvfbController) throws IOException {
        WebDriverFactory factory = createCookielessWebDriverFactory(xvfbController);
        HarPlus<Void> collection = new HttpsTestTrafficCollector(factory).collect(driver -> {
            System.out.format("visiting: %s%n", url);
            driver.get(url.toString());
            return (Void) null;
        });
        List<DeserializableCookie> cookies = HarAnalysis.of(collection.har).findCookies(FlexibleCookieSpec.getDefault());
        checkOurCookies(cookies);
        System.out.format("cookies received: %s%n", cookies);
        checkState(!cookies.isEmpty(), "expected at least one cookie to be set");
        return cookies;
    }

    private void checkOurCookies(Iterable<DeserializableCookie> cookies) {
        for (DeserializableCookie c : cookies) {
            checkArgument(c.getExpiryDate() != null, "null expiry: %s", c);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void exerciseCookieCapabilities() throws IOException, WebDriverException {
        Har har;
        final URL cookieSetUrl = new URL("https://httprequestecho.appspot.com/cookies/set");
        final URL cookieGetUrl = new URL("https://httprequestecho.appspot.com/get");
        final Multimap<String, String> cookieHeaderValues = ArrayListMultimap.create();
        List<DeserializableCookie> cookiesSetByServer;
        try (XvfbController xvfbController = xvfb.getController()) {
            cookiesSetByServer = browseAndSetCookies(cookieSetUrl, xvfbController);
            System.out.format("creating webdriver factory with %s%n", cookiesSetByServer);
            WebDriverFactory factory = createCookiefulWebDriverFactory(xvfbController, cookiesSetByServer);
            har = new HttpsTestTrafficCollector(factory).collect(driver -> {
                driver.get("http://www.example.com/");
                sleepQuietly(1000);
                driver.get(cookieGetUrl.toString());
                return (Void) null;
            }, new TrafficListener() {
                @Override
                public void responseReceived(HttpResponse response) {
                }

                @Override
                public void sendingRequest(HttpRequest request) {
                    String uri = request.getUri();
                    if (cookieGetUrl.getPath().equals(uri)) {
                        System.out.format("sending request to %s with headers %s%n", uri, request.headers().names());
                    }
                    cookieHeaderValues.putAll(request.getUri(), request.headers().getAll(HttpHeaders.COOKIE));
                }
            }).har;
        }
        System.out.format("cookie header values for paths %s: %s%n", cookieHeaderValues.keySet(), cookieHeaderValues);
        Stream<HarEntry> entries = har.getLog().getEntries().stream().filter(harEntry -> cookieGetUrl.toString().equals(harEntry.getRequest().getUrl()));
        List<String> cookiesSent = entries.flatMap(entry -> entry.getRequest().getHeaders().stream()).filter(header -> HttpHeaders.COOKIE.equalsIgnoreCase(header.getName())).map(HarNameValuePair::getValue).collect(Collectors.toList());
        System.out.format("sent: %s%n", cookiesSent);
        assertFalse("expected at least one cookie to have been sent", cookiesSent.isEmpty());

        for (DeserializableCookie cookieSetByServer : cookiesSetByServer) {
            boolean foundMatch = false;
            String expectedContents = String.format("%s=%s", cookieSetByServer.getName(), cookieSetByServer.getValue());
            for (String cookieSent : cookiesSent) {
                if (cookieSent.contains(expectedContents)) {
                    foundMatch = true;
                    break;
                }
            }
            assertEquals("expected some sent cookie to match " + expectedContents, true, foundMatch);
        }
        System.out.println();
    }

    protected abstract WebDriverFactory createCookiefulWebDriverFactory(XvfbController xvfbController, List<DeserializableCookie> cookiesSetByServer);

    static DeserializableCookie newCookie(String name, String value) {
        DeserializableCookie cookie = new DeserializableCookie();
        cookie.setHttpOnly(true);
        Date now = new Date();
        Date later = DateUtils.addMonths(now, 3);
        cookie.setCreationDate(now);
        cookie.setLastAccessed(now);
        cookie.setExpiryDate(later);
        cookie.setSecure(true);
        cookie.setDomain("localhost");
        cookie.setName(name);
        cookie.setValue(value);
        cookie.setPath("/");
        return cookie;
    }


}