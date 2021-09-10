package io.github.mike10004.seleniumcapture.testbases;

import com.github.mike10004.seleniumhelp.ImmutableHttpResponse;
import com.github.mike10004.seleniumhelp.TrafficCollector;
import com.github.mike10004.seleniumhelp.TrafficGenerator;
import com.github.mike10004.seleniumhelp.WebDriverFactory;
import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import io.github.mike10004.nanochamp.repackaged.fi.iki.elonen.NanoHTTPD;
import io.github.mike10004.nanochamp.server.NanoControl;
import io.github.mike10004.nanochamp.server.NanoResponse;
import io.github.mike10004.nanochamp.server.NanoServer;
import io.github.mike10004.nitsick.junit.TimeoutRules;
import io.github.mike10004.seleniumcapture.testing.UnitTests;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for the brotli-aware filter. Tests with Firefox and Chrome.
 */
public abstract class BrAwareServerResponseCaptureFilterTest {

    public static abstract class ThisTestBase {

        @Rule
        public final Timeout timeout = TimeoutRules.from(UnitTests.Settings).getLongRule();

        @Rule
        public final XvfbRule xvfb = UnitTests.xvfbRuleBuilder().build();

        protected final WebDriverTestParameter testParameter;

        public ThisTestBase(WebDriverTestParameter testParameter) {
            this.testParameter = testParameter;
        }

        @Before
        public void setUp() {
            testParameter.doDriverManagerSetup();
        }

        protected static byte[] checkInteractions(String url, RecordingMonitor monitor) throws IOException {
            assertFalse("expect nonempty interactions list", monitor.interactions.isEmpty());
            ImmutableHttpResponse response = monitor.interactions.stream()
                    .filter(pair -> url.equals(pair.request.url.toString()))
                    .map(HttpInteraction::getResponse).findAny().orElse(null);
            assertNotNull("response not found among " + monitor.interactions + "\n(searched for request URL " + url + ")", response);
            System.out.format("response: %s GET %s%n", response.status, url);
            response.headers.forEach((name, value) -> System.out.format("%s: %s%n", name, value));
            assertEquals("response status", 200, response.status);
            List<String> contentEncodingHeaderValues = response.getHeaders("content-encoding")
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
            assertTrue("expect value of 'content-encoding' is 'br'", contentEncodingHeaderValues.stream().allMatch("br"::equals));
            byte[] harResponseBytes = response.getContentAsBytes().read();
            String harResponseBytesHex = new String(new Hex().encode(harResponseBytes));
            System.out.format("response bytes captured by monitor filter:%nbase-16 %s%nbase-10 %s%n",
                    StringUtils.abbreviateMiddle(harResponseBytesHex, "...", 256),
                    StringUtils.abbreviateMiddle(Arrays.toString(harResponseBytes), "...", 256));
            return harResponseBytes;
        }
    }



    public static abstract class LocalTestBase extends ThisTestBase {

        public LocalTestBase(WebDriverTestParameter testParameter) {
            super(testParameter);
        }


        @Test
        public void testMonitorCapturesBrotliResponses_local() throws Exception {
            byte[] brotliBytes = TestBases.loadBrotliCompressedSample();
            byte[] decompressedBytes = TestBases.loadBrotliUncompressedSample();
            String decompressedText = new String(decompressedBytes, UTF_8);
            WebDriverFactory webDriverFactory = testParameter.createWebDriverFactory(xvfb);
            TrafficCollector collector = TrafficCollector.builder(webDriverFactory)
                    .build();
            RecordingMonitor monitor = new RecordingMonitor();
            NanoHTTPD.Response compressedResponse = NanoResponse.status(200)
                    .header(HttpHeaders.CONTENT_ENCODING, "br")
                    .content(MediaType.PLAIN_TEXT_UTF_8, brotliBytes)
                    .build();
            System.out.format("prepared response with compressed bytes: %s%n", new String(new Hex().encode(brotliBytes)));
            NanoServer server = NanoServer.builder().get(session -> compressedResponse).build();
            String pageText;
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
                pageText = collector.monitor(generator, monitor);
            }
            byte[] harResponseBytes = checkInteractions(url, monitor);
            assertArrayEquals("response byte", decompressedBytes, harResponseBytes);
            if (testParameter.isBrotliSupported(url)) {
                assertEquals("pageText", decompressedText, pageText);
            }
        }
    }

    private static final String SYSPROP_REMOTE_BROLIT_RESOURCE_URL = "selenium-capture.tests.remoteBrotliResourceUrl";
    private static final String DEFAULT_REMOTE_BROTLI_RESOURCE_URL = "https://httpbin.org/brotli";

    public static abstract class RemoteTestBase extends ThisTestBase {

        public RemoteTestBase(WebDriverTestParameter testParameter) {
            super(testParameter);
        }

        @Test
        public void testMonitorCapturesBrotliResponses_remote() throws Exception {
            WebDriverFactory webDriverFactory = testParameter.createWebDriverFactory(xvfb);
            TrafficCollector collector = TrafficCollector.builder(webDriverFactory).build();
            RecordingMonitor monitor = new RecordingMonitor();
            String url = UnitTests.Settings.get(SYSPROP_REMOTE_BROLIT_RESOURCE_URL);
            if (url == null) {
                url = DEFAULT_REMOTE_BROTLI_RESOURCE_URL;
            }
            final String finalUrl = url;
            TrafficGenerator<String> generator = driver -> {
                driver.get(finalUrl);
                WebElement body = driver.findElement(By.tagName("body"));
                String text = body.getText();
                return text;
            };
            collector.monitor(generator, monitor);
            checkInteractions(finalUrl, monitor);
        }
    }

}