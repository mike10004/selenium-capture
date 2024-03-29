package io.github.mike10004.seleniumcapture.testbases;

import com.browserup.harreader.model.Har;
import com.browserup.harreader.model.HarEntry;
import com.browserup.harreader.model.HarHeader;
import io.github.mike10004.seleniumcapture.DeserializableCookie;
import io.github.mike10004.seleniumcapture.HarAnalysis;
import io.github.mike10004.seleniumcapture.HarPlus;
import io.github.mike10004.seleniumcapture.SetCookieHeaderParser;
import io.github.mike10004.seleniumcapture.TrafficCollector;
import io.github.mike10004.seleniumcapture.WebDriverFactory;
import com.github.mike10004.xvfbmanager.XvfbController;
import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.net.HttpHeaders;
import io.github.mike10004.seleniumcapture.testing.UnitTests;
import org.junit.Rule;
import org.openqa.selenium.WebDriverException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public abstract class CookieUsageTestBase {

    @Rule
    public XvfbRule xvfb = UnitTests.xvfbRuleBuilder().build();

    protected abstract WebDriverFactory createCookielessWebDriverFactory(XvfbController xvfbController) throws IOException;

    private List<DeserializableCookie> browseAndSetCookies(URL url, XvfbController xvfbController) throws IOException {
        WebDriverFactory factory = createCookielessWebDriverFactory(xvfbController);
        HarPlus<Void> collection = TrafficCollector.builder(factory).collectHttps(TestCertificateAndKeySource.create())
                .build().collect(driver -> {
            System.out.format("visiting: %s%n", url);
            driver.get(url.toString());
            return (Void) null;
        });
        List<DeserializableCookie> cookies = HarAnalysis.of(collection.har).findCookies(SetCookieHeaderParser.create()).makeUltimateCookieList();
        checkOurCookies(cookies);
        System.out.format("cookies received: %s%n", cookies);
        checkState(!cookies.isEmpty(), "expected at least one cookie to be set");
        return cookies;
    }

    protected void checkOurCookies(Iterable<DeserializableCookie> cookies) {
        for (DeserializableCookie c : cookies) {
            checkArgument(c.getExpiryInstant() != null, "null expiry: %s", c);
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

    private static boolean isRequestToUrl(URL reference, URI probe) {
        try {
            return reference.toURI().equals(probe);
        } catch (URISyntaxException e) {
            e.printStackTrace(System.out);
            return false;
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

            har = TrafficCollector.builder(factory)
                    .collectHttps(TestCertificateAndKeySource.create())
                    .build().collect(driver -> {
                driver.get("http://www.example.com/");
                sleepQuietly(1000);
                driver.get(cookieGetUrl.toString());
                return (Void) null;
            }, (request, response) -> {
                if (isRequestToUrl(cookieGetUrl, request.url)) {
                    System.out.format("sending request to %s with headers %s%n", request.url, request.headers.keySet());
                }
                cookieHeaderValues.putAll(request.url.toString(), request.getHeaderValues(HttpHeaders.COOKIE).collect(Collectors.toList()));
            }).har;
        } catch (WebDriverException e) {
            System.err.println("exerciseCookieCapabilities failed due to " + e.getClass().getName());
            e.printStackTrace(System.err);
            throw e;
        }
        browsingFinished(cookieHeaderValues, cookieGetUrl, har, cookiesSetByServer);
    }

    protected void browsingFinished(Multimap<String, String> cookieHeaderValues, URL cookieGetUrl, Har har, List<DeserializableCookie> cookiesSetByServer) {
        System.out.format("cookie header values for paths %s: %s%n", cookieHeaderValues.keySet(), cookieHeaderValues);
        Stream<HarEntry> entries = har.getLog().getEntries().stream().filter(harEntry -> cookieGetUrl.toString().equals(harEntry.getRequest().getUrl()));
        List<String> cookiesSent = entries.flatMap(entry -> entry.getRequest().getHeaders().stream())
                .filter(header -> HttpHeaders.COOKIE.equalsIgnoreCase(header.getName()))
                .map(HarHeader::getValue).collect(Collectors.toList());
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

    protected abstract WebDriverFactory createCookiefulWebDriverFactory(XvfbController xvfbController, List<DeserializableCookie> cookiesSetByServer) throws IOException;

}
