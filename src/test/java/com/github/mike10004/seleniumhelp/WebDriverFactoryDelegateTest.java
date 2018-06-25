package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.mike10004.nanochamp.server.NanoControl;
import io.github.mike10004.nanochamp.server.NanoResponse;
import io.github.mike10004.nanochamp.server.NanoServer;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

/**
 * This test was intended to confirm a bug in firefox cookie management, but instead it turns out it just reveals a
 * quirk. The quirk is that Firefox requires the DeserializableCookie's domain attribute to be set, in addition
 * to the cookieDomain field. The test can probably be deleted, but it should be noted in the documentation somewhere
 * that Firefox has this quirk.
 */
@org.junit.Ignore
public class WebDriverFactoryDelegateTest {

    @ClassRule
    public static WebDriverManagerRule chromeRule = WebDriverManagerRule.chromedriver();

    @ClassRule
    public static WebDriverManagerRule firefoxRule = WebDriverManagerRule.geckodriver();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public XvfbRule xvfbRule = XvfbRule.builder().build();

    @Test
    public void useLocalhostCookie_chrome() throws Exception {
        Path scratchDir = temporaryFolder.getRoot().toPath();
        testUseLocalhostCookie((env, cookies) -> {
            return ChromeWebDriverFactory.builder()
                    .cookiePreparer(ChromeWebDriverFactory.makeCookieImplanter(scratchDir, () -> cookies))
                    .environment(env)
                    .build();
        });
    }

    @Test
    public void useLocalhostCookie_firefox() throws Exception {
        testUseLocalhostCookie((env, cookies) -> {
            return FirefoxWebDriverFactory.builder()
                    .addCookies(cookies)
                    .environment(env)
                    .build();
        });
    }

    private interface FactoryFactory {
        WebDriverFactory create(Map<String, String> environment, List<DeserializableCookie> cookies);
    }

    private void testUseLocalhostCookie(FactoryFactory webDriverFactoryFactory) throws Exception {
//        DeserializableCookie cookie = DeserializableCookie.builder("foo", "bar")
//                .domain("localhost")
//                .path("/")
//                .expiry(Date.from(Instant.now().plus(Duration.ofDays(7))))
//                .build();
//        List<DeserializableCookie> cookies = Collections.singletonList(cookie);
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        String cookieName = "myCookie";
        NanoServer server = NanoServer.builder()
                .getPath("/", request -> {
                    String[] cookieStuffs = {};
                    String cookieHeaderVal = request.headers.apply(HttpHeaders.COOKIE);
                    if (cookieHeaderVal != null) {
                        cookieStuffs = cookieHeaderVal.split("\\s*;\\s*");
                    }
                    return NanoResponse.status(200).jsonUtf8(gson.toJson(cookieStuffs));
                }).getPath("/setcookie", request -> {
                    String hval = makeSetCookie(cookieName, UUID.randomUUID().toString(), 3000);
                    return NanoResponse.status(200)
                            .header(HttpHeaders.SET_COOKIE, hval)
                            .plainTextUtf8("cookie set: " + hval);
                })
                .build();
        List<DeserializableCookie> cookies = makeCookies(server, webDriverFactoryFactory);
        new GsonBuilder().setPrettyPrinting().create().toJson(cookies, System.out);
        System.out.println();
        String pageSource = useCookies(server, webDriverFactoryFactory, cookies);
        assertTrue("pageSource has cookie", pageSource.contains(cookieName));

    }

    @SuppressWarnings("unused")
    private List<DeserializableCookie> makeCookies(NanoServer server, FactoryFactory webDriverFactoryFactory) {
        DeserializableCookie cookie = DeserializableCookie.builder("myCookie", "blahblahblah")
                .expiry(Date.from(Instant.now().plus(Duration.ofDays(365))))
                .path("/")
                .domain("localhost")
                .attribute("domain", "localhost")
                .build();
        return ImmutableList.of(cookie);
    }

    private String useCookies(NanoServer server, FactoryFactory webDriverFactoryFactory, List<DeserializableCookie> cookies) throws Exception {
        String pageSource;
        {
            WebDriverFactory webDriverFactory2 = webDriverFactoryFactory.create(xvfbRule.getController().newEnvironment(), cookies);
            TrafficCollector collector2 = TrafficCollector.builder(webDriverFactory2).build();
            // make the cookies
            try (NanoControl ctrl = server.startServer()) {
                String url = String.format("http://%s/", ctrl.getSocketAddress());
                System.out.println(url);
                pageSource = collector2.drive(driver -> {
                    driver.get(url);
                    return driver.getPageSource();
                });
            }
        }
        System.out.println(pageSource);
        return pageSource;
    }

    @SuppressWarnings("SameParameterValue")
    private static String makeSetCookie(String name, String value, int maxAge) {
        return String.format("%s=%s; Max-Age=%d; path=/; domain=localhost", name, value, maxAge);
    }
}