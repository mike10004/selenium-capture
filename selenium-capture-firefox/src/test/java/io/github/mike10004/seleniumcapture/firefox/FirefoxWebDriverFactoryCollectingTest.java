package io.github.mike10004.seleniumcapture.firefox;

import io.github.mike10004.seleniumcapture.ImmutableHttpRequest;
import io.github.mike10004.seleniumcapture.ImmutableHttpResponse;
import io.github.mike10004.seleniumcapture.TrafficCollector;
import io.github.mike10004.seleniumcapture.TrafficGenerator;
import io.github.mike10004.seleniumcapture.TrafficMonitor;
import io.github.mike10004.seleniumcapture.testbases.HttpInteraction;
import io.github.mike10004.seleniumcapture.testbases.TestCertificateAndKeySource;
import io.github.mike10004.seleniumcapture.testbases.WebDriverManagerRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FirefoxWebDriverFactoryCollectingTest {


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
