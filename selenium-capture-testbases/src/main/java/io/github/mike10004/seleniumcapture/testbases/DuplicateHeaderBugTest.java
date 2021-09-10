package io.github.mike10004.seleniumcapture.testbases;

import io.github.mike10004.seleniumcapture.HarInteractions;
import io.github.mike10004.seleniumcapture.HarPlus;
import io.github.mike10004.seleniumcapture.ImmutableHttpResponse;
import io.github.mike10004.seleniumcapture.TrafficCollector;
import io.github.mike10004.seleniumcapture.TrafficGenerator;
import io.github.mike10004.seleniumcapture.WebDriverFactory;
import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import io.github.mike10004.nanochamp.repackaged.fi.iki.elonen.NanoHTTPD;
import io.github.mike10004.nanochamp.server.NanoControl;
import io.github.mike10004.nanochamp.server.NanoResponse;
import io.github.mike10004.nanochamp.server.NanoServer;
import io.github.mike10004.nitsick.junit.TimeoutRules;
import com.browserup.harreader.model.HarEntry;
import io.github.mike10004.seleniumcapture.testing.UnitTests;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public abstract class DuplicateHeaderBugTest {

    @Rule
    public final Timeout timeout = TimeoutRules.from(UnitTests.Settings).getLongRule();

    @Rule
    public final XvfbRule xvfb = UnitTests.xvfbRuleBuilder().build();

    private final WebDriverTestParameter testParameter;

    public DuplicateHeaderBugTest(WebDriverTestParameter testParameter) {
        this.testParameter = testParameter;
    }

    @Before
    public void setUpWebDriver() {
        testParameter.doDriverManagerSetup();
    }

    private interface Verifier<T> {
        T invoke(TrafficCollector collector, TrafficGenerator<String> generator) throws IOException;
        void verify(String url, T content) throws Exception;
        default void checkHeaders(ImmutableHttpResponse response) {
            expectedHeaders.forEach((name, value) -> {
                List<String> values = response.getHeaders(name).map(Map.Entry::getValue).collect(Collectors.toList());
                assertEquals("expect exactly one header by name", 1, values.size());
                assertEquals("header value", value, values.get(0));
            });
        }
    }

    @Test
    public void testHarContainsEachHeaderExactlyOnce() throws Exception {
        testEachHeaderCollectedExactlyOnce(new Verifier<HarPlus<String>>(){
            @Override
            public HarPlus<String> invoke(TrafficCollector collector, TrafficGenerator<String> generator) throws IOException {
                return collector.collect(generator);
            }

            @Override
            public void verify(String url, HarPlus<String> content) throws Exception {
                checkState(pageContent.equals(content.result), "unexpected page text: %s", content);
                ImmutableHttpResponse response = content.har.getLog().getEntries().stream()
                        .filter(entry -> url.equals(entry.getRequest().getUrl()))
                        .map(HarEntry::getResponse)
                        .map(HarInteractions::freeze)
                        .findFirst()
                        .orElse(null);
                assertNotNull("expect to find har response matching url " + url, response);
                checkHeaders(response);
            }
        });
    }

    @Test
    public void testMonitorCollectsEachHeaderExactlyOnce() throws Exception {
        RecordingMonitor monitor = new RecordingMonitor();
        testEachHeaderCollectedExactlyOnce(new Verifier<String>(){
            @Override
            public String invoke(TrafficCollector collector, TrafficGenerator<String> generator) throws IOException {
                return collector.monitor(generator, monitor);
            }

            @Override
            public void verify(String url, String content) throws Exception {
                checkState(pageContent.equals(content), "unexpected page text: %s", content);
                assertFalse("expect nonempty interactions list", monitor.interactions.isEmpty());
                ImmutableHttpResponse response = monitor.interactions.stream()
                        .filter(pair -> url.equals(pair.request.url.toString()))
                        .map(HttpInteraction::getResponse).findAny().orElse(null);
                assertNotNull("response not found among " + monitor.interactions + "\n(searched for request URL " + url + ")", response);
                checkHeaders(response);
            }
        });
    }



    private static final String pageContent = "hola mundo";
    private static final String uniqueHeaderName = "X-Selenium-Help-Unit-Test-Foo", uniqueHeaderValue = "134568913460971460";
    private static final Map<String, String> expectedHeaders = ImmutableMap.<String, String>builder()
            .put("content-type", MediaType.PLAIN_TEXT_UTF_8.toString())
            .put(uniqueHeaderName, uniqueHeaderValue)
            .build();

    private <T> void testEachHeaderCollectedExactlyOnce(Verifier<T> verifier) throws Exception {
        WebDriverFactory webDriverFactory = testParameter.createWebDriverFactory(xvfb);
        TrafficCollector collector = TrafficCollector.builder(webDriverFactory)
                .build();
        NanoHTTPD.Response aResponse = NanoResponse.status(200)
                .header(uniqueHeaderName, uniqueHeaderValue)
                .content(MediaType.PLAIN_TEXT_UTF_8, pageContent)
                .build();
        NanoServer server = NanoServer.builder().get(session -> aResponse).build();
        T pageText;
        String url;
        try (NanoControl ctrl = server.startServer()) {
            url = ctrl.baseUri().toString();
            TrafficGenerator<String> generator = driver -> {
                try {
                    driver.get(url);
                    WebElement body = driver.findElement(By.tagName("body"));
                    String text = body.getText();
                    return text;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            };
            pageText = verifier.invoke(collector, generator);
        }
        verifier.verify(url, pageText);
    }
}
