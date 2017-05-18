package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbtesting.XvfbRule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Test for the brotli-aware filter. Uses Chrome because JBrowserDriver does not support brotli decoding.
 */
public class BrAwareServerResponseCaptureFilterTest {

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    @BeforeClass
    public static void initChromeDriver() {
        ChromeDriverManager.getInstance().setup(UnitTests.RECOMMENDED_CHROMEDRIVER_VERSION);
    }

    @Rule
    public final XvfbRule xvfb = XvfbRule.builder().disabledOnWindows().build();

    @Test(timeout = 10000L)
    public void endToEnd_chrome() throws Exception {
        byte[] brotliBytes = loadBrotliCompressedSample();
        byte[] decompressedBytes = loadUncompressedSample();
        String decompressedText = new String(decompressedBytes, UTF_8);
        String path = "/compressed";
        wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo(path))
                .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_ENCODING, "br")
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString())
                        .withBody(brotliBytes)));
        wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/favicon.ico"))
                .willReturn(WireMock.aResponse().withStatus(404).withBody("not found")));
        String url = String.format("http://localhost:%d%s", wireMockRule.port(), path);
        WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                .environment(xvfb.getController().newEnvironment())
                .build();
        TrafficCollector collector = TrafficCollector.builder(webDriverFactory).build();
        TrafficGenerator<String> generator = driver -> {
            driver.get(url);
            WebElement body = driver.findElement(By.tagName("body"));
            return body.getText();
        };
        List<Pair<ImmutableHttpRequest, ImmutableHttpResponse>> responses = new ArrayList<>();
        String pageText = collector.monitor(generator, (req, rsp) -> responses.add(Pair.of(req, rsp)));
        ImmutableHttpResponse response = responses.stream()
                .filter(pair -> pair.getLeft().url.getPath().equals(path))
                .map(Pair::getRight).findAny().get();
        System.out.format("page text:%n%s%n%n", StringUtils.abbreviate(pageText, 256));
        assertEquals("pageText", decompressedText, pageText);
        assertArrayEquals("response byte", decompressedBytes, response.getContentAsBytes().read());
    }

    public static class WithoutMockServerTest {

        @Test
        public void decompressBrotliContents() throws Exception {
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, String.format("http://localhost:%d/blah", 12345));
            BrAwareServerResponseCaptureFilter filter = new BrAwareServerResponseCaptureFilter(request, true);
            byte[] brotliBytes = loadBrotliCompressedSample();
            byte[] decompressedBytes = filter.decompressBrotliContents(brotliBytes);
            byte[] expected = loadUncompressedSample();
            assertArrayEquals("bytes", expected, decompressedBytes);
        }
    }


    private static byte[] loadUncompressedSample() throws IOException {
        return Resources.toByteArray(BrAwareServerResponseCaptureFilterTest.class.getResource("/brotli/a100.txt"));
    }

    private static byte[] loadBrotliCompressedSample() throws IOException {
        return Resources.toByteArray(BrAwareServerResponseCaptureFilterTest.class.getResource("/brotli/a100.txt.br"));
    }
}