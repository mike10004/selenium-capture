package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(Enclosed.class)
public abstract class FirefoxWebDriverFactoryTest {

    public static class NonCollectingTest {

        @Test
        public void checkPreferencesValues() {
            FirefoxWebDriverFactory.checkPreferencesValues(ImmutableList.of()); // these are ok if no exception is thrown
            FirefoxWebDriverFactory.checkPreferencesValues(ImmutableList.of(1));
            FirefoxWebDriverFactory.checkPreferencesValues(ImmutableList.of("foo"));
            FirefoxWebDriverFactory.checkPreferencesValues(ImmutableList.of(true));
            FirefoxWebDriverFactory.checkPreferencesValues(ImmutableList.of(1, false, "bar"));
        }

        @Test(expected = IllegalArgumentException.class)
        public void checkPreferencesValues_null() {
            FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList((String) null));
        }

        @Test(expected = IllegalArgumentException.class)
        public void checkPreferencesValues_object() {
            FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList(new Object()));
        }

        private static class Widget {
        }

        @Test(expected = IllegalArgumentException.class)
        public void checkPreferencesValues_widget() {
            FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList(new Widget()));
        }

        @Test(expected = IllegalArgumentException.class)
        public void checkPreferencesValues_dobule() {
            FirefoxWebDriverFactory.checkPreferencesValues(Collections.singletonList(1.5));
        }

        @Test
        public void usesEnvironment() {
            Map<String, String> expected = ImmutableMap.of("foo", "bar");
            // we don't actually launch Firefox, so we don't need to apply UnitTests.createFirefoxBinarySupplier()
            FirefoxWebDriverFactory factory = FirefoxWebDriverFactory.builder()
                    .environment(expected)
                    .build();
            Map<String, String> actual = factory.environmentSupplier.get();
            assertEquals("environment", expected, actual);
        }
    }

    public static class CollectingTest {


        @ClassRule
        public static TemporaryFolder temporaryFolder = new TemporaryFolder();

        @ClassRule
        public static WebDriverManagerRule webDriverManagerRule = WebDriverManagerRule.geckodriver();

        @Test
        public void disablesAutoPhoneHome() throws Exception {
            FirefoxWebDriverFactory factory = FirefoxWebDriverFactory.builder()
                    .binary(FirefoxUnitTests.createFirefoxBinarySupplier())
                    .configure(o -> o.setHeadless(true))
                    .scratchDir(temporaryFolder.getRoot().toPath())
                    .acceptInsecureCerts()
                    .profileAction(profile -> {
                        profile.setPreference("browser.chrome.favicons", false);
                        profile.setPreference("browser.chrome.site_icons", false);
                    })
                    .build();
            TrafficCollector collector = TrafficCollector.builder(factory).collectHttps(TestCertificateAndKeySource.create()).build();
            List<HttpInteraction> responses = Collections.synchronizedList(new ArrayList<>());
            String url = "https://httpbin.org/get";
            collector.monitor(new TrafficGenerator<Void>() {
                @Override
                public Void generate(WebDriver driver) {
                    driver.get(url);
                    try {
                        // allow some time for phoning home
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                    }
                    return null;
                }
            }, new TrafficMonitor() {
                @Override
                public void responseReceived(ImmutableHttpRequest httpRequest, ImmutableHttpResponse httpResponse) {
                    String chars = "<not_decoded>";
                    try {
                        chars = httpResponse.getContentAsChars().read();
                    } catch (Exception e) {
                        System.err.format("not decoded: %s -> %s%n", httpRequest, httpResponse);
                    }
                    System.out.format("%s %s -> length %s%n%n%s%n", httpRequest.method, httpRequest.url,
                            httpResponse.getFirstHeaderValue("content-length"),
                            chars);
                    responses.add(new HttpInteraction(httpRequest, httpResponse));
                }
            });
            assertEquals("responses", 1, responses.size());
            assertEquals("request URL", url, responses.get(0).getRequest().url.toString());
        }
    }
}