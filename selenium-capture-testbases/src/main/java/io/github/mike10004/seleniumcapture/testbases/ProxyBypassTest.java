package io.github.mike10004.seleniumcapture.testbases;

import io.github.mike10004.nanochamp.server.HostAddress;
import io.github.mike10004.seleniumcapture.FullSocketAddress;
import io.github.mike10004.seleniumcapture.ProxyDefinitionBuilder;
import io.github.mike10004.seleniumcapture.ProxyDefinition;
import io.github.mike10004.seleniumcapture.TrafficCollector;
import io.github.mike10004.seleniumcapture.TrafficMonitor;
import io.github.mike10004.seleniumcapture.UpstreamProxyDefinition;
import io.github.mike10004.seleniumcapture.WebdrivingConfig;
import io.github.mike10004.seleniumcapture.WebdrivingProxyDefinition;
import io.github.mike10004.seleniumcapture.WebdrivingSession;
import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import io.github.mike10004.nanochamp.server.NanoControl;
import io.github.mike10004.nanochamp.server.NanoResponse;
import io.github.mike10004.nanochamp.server.NanoServer;
import io.github.mike10004.seleniumcapture.testing.UnitTests;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.filters.HttpsAwareFiltersAdapter;
import org.jsoup.Jsoup;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class ProxyBypassTest {

    private final WebDriverTestParameter testParameter;

    public ProxyBypassTest(WebDriverTestParameter testParameter) {
        this.testParameter = testParameter;
    }

    @ClassRule
    public static final XvfbRule xvfbRule = UnitTests.xvfbRuleBuilder().build();

    @Before
    public void setUpWebdriver() {
        testParameter.doDriverManagerSetup();
    }

    /**
     * Exercises a webdriver with an intercepting proxy, confirming that we can configure the webdriver
     * to bypass the proxy for certain addresses.
     */
    @Test
    public void webdrivingProxy_bypassLocalhost() throws Exception {
        System.out.format("bypass: testing with %s%n", testParameter.getClass().getSimpleName());
        Map<URI, String> bodyTexts = testWebdrivingBypass(host -> true);
        assertEquals("expect to be bypassed", Collections.singleton(MESSAGE_NOT_INTERCEPTED), new HashSet<>(bodyTexts.values()));
    }

    /**
     * Makes sure we're not getting a false positive on the {@link #webdrivingProxy_bypassLocalhost()} test.
     * This exercises the webdriver with an intercepting proxy and no bypasses, and it makes sure that we get only
     * what the proxy serves.
     */
    @Test
    public void webdrivingProxy_nobypass() throws Exception {
        System.out.format("nobypass: testing with %s%n", testParameter.getClass().getSimpleName());
        Map<URI, String> bodyTexts = testWebdrivingBypass(host -> false);
        assertEquals("expect no bypass", Collections.singleton(MESSAGE_INTERCEPTED), new HashSet<>(bodyTexts.values()));
    }

    private static final String MESSAGE_NOT_INTERCEPTED = "Reached the target server", MESSAGE_INTERCEPTED = "Intercepted by proxy";
    private static final int MAX_BUFFER_SIZE_BYTES = 0; //2 * 1024 * 1024;

    private Map<URI, String> testWebdrivingBypass(Predicate<? super String> bypassFilter) throws Exception {
        NanoServer server1 = NanoServer.builder()
                .get(whatever -> NanoResponse.status(200).plainTextUtf8(MESSAGE_NOT_INTERCEPTED))
                .build();
        NanoServer server2 = NanoServer.builder()
                .get(whatever -> NanoResponse.status(200).plainTextUtf8(MESSAGE_NOT_INTERCEPTED))
                .build();
        BrowserUpProxy proxy = new BrowserUpProxyServer();
        proxy.addLastHttpFilterFactory(new InterceptingFiltersSource());
        ExecutorService executorService = (Executors.newSingleThreadExecutor());
        try {
            proxy.start();
            try {
                String proxyHost = "127.0.0.1";
                int proxyPort = proxy.getPort();
                try (NanoControl ctrl1 = server1.startServer();
                     NanoControl ctrl2 = server2.startServer()) {
                    HostAddress targetSocketAddress1 = ctrl1.getSocketAddress();
                    HostAddress targetSocketAddress2 = ctrl2.getSocketAddress();
                    List<String> bypasses = Stream.of(targetSocketAddress1, targetSocketAddress2)
                            .map(Object::toString)
                            .filter(bypassFilter)
                            .collect(Collectors.toList());
                    WebdrivingProxyDefinition webdrivingProxyDefinition = ProxyDefinitionBuilder
                            .through(FullSocketAddress.define(proxyHost, proxyPort))
                            .addProxyBypasses(bypasses)
                            .http();
                    WebdrivingConfig config = WebdrivingConfig.builder()
                            .proxy(webdrivingProxyDefinition)
                            .build();
                    URI[] urls = {
                            URI.create(String.format("http://%s/", targetSocketAddress1)),
                            URI.create(String.format("http://%s/", targetSocketAddress2)),
                    };
                    Map<URI, String> texts = new LinkedHashMap<>();
                    try (WebdrivingSession session = testParameter.createWebDriverFactory(xvfbRule).startWebdriving(config)) {
                        WebDriver driver = session.getWebDriver();
                        for (URI url : urls) {
                            Future<String> promise;
                            System.out.println("waiting 5 seconds for response...");
                            List<Future<String>> promises = executorService.invokeAll(Collections.singleton(new BodyTextFetcher(driver, url)), 5, TimeUnit.SECONDS);
                            assertEquals("num promises", 1, promises.size());
                            promise = promises.get(0);
                            if (!promise.isCancelled()) {
                                String value = promise.get();
                                System.out.format("returned \"%s\"%n", value);
                                texts.put(url, value.trim());
                            } else {
                                System.out.format("no response from %s due to timeout%n", url);
                                texts.put(url, "");
                            }
                        }
                        executorService.shutdown(); // no more tasks to be submitted
                    } finally {
                        System.out.format("webdriving session closing%n");
                    }
                    return texts;
                }
            } finally {
                proxy.stop();
            }
        } finally {
            boolean executorServiceStopped = executorService.awaitTermination(10, TimeUnit.SECONDS);
            checkState(executorServiceStopped, "executorServiceStopped");
        }
    }

    private static class BodyTextFetcher implements Callable<String> {
        private final WebDriver driver;
        private final URI url;

        public BodyTextFetcher(WebDriver driver, URI url) {
            this.driver = driver;
            this.url = url;
        }

        @Override
        public String call() {
            String url = this.url.toString();
            System.out.format("using %s to fetch %s%n", driver.getClass().getSimpleName(), url);
            driver.get(url);
            try {
                WebElement body = driver.findElement(By.tagName("body"));
                return Strings.nullToEmpty(body.getText());
            } catch (WebDriverException e) {
                e.printStackTrace(System.out);
                throw e;
            }
        }
    }

    private static class InterceptingFiltersSource extends HttpFiltersSourceAdapter {
        @Override
        public int getMaximumRequestBufferSizeInBytes() {
            return MAX_BUFFER_SIZE_BYTES;
        }

        @Override
        public int getMaximumResponseBufferSizeInBytes() {
            return MAX_BUFFER_SIZE_BYTES;
        }

        @Override
        public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
            return new HttpsAwareFiltersAdapter(originalRequest, ctx) {
                @Override
                public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                    HttpResponse response = super.clientToProxyRequest(httpObject);
                    if (!isHttps() && httpObject instanceof HttpRequest) {
                        HttpRequest request = (HttpRequest) httpObject;
                        URI uri = URI.create((request).uri());
                        if (HttpMethod.GET.equals(request.method()) && "/".equals(uri.getPath())) {
                            Charset charset = StandardCharsets.UTF_8;
                            byte[] bytes = MESSAGE_INTERCEPTED.getBytes(charset);
                            DefaultFullHttpResponse response_ = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.BAD_REQUEST, Unpooled.wrappedBuffer(bytes));
                            response_.headers().set(HttpHeaders.CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.withCharset(charset).toString());
                            response_.headers().set(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length));
                            response = response_;
                        }
                    }
                    System.out.format("%s %s -> %s%n", response == null ? "passthru" : "intercept", describe(httpObject), describe(response));
                    return response;
                }
            };

        }

    }

    private static String describe(HttpObject httpObject) {
        if (httpObject == null) {
            return "null";
        }
        if (httpObject instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) httpObject;
            return String.format("%s %s %s", request.getClass().getSimpleName(), request.method(), request.uri());
        }
        if (httpObject instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) httpObject;
            return String.format("%s %s Content-Type: %s", response.getClass().getSimpleName(), response.status().code(), response.headers().get("Content-Type"));
        }
        return httpObject.toString();
    }

    private static String toHostAndPort(URI httpServerBaseUri) {
        return httpServerBaseUri.getHost() + ":" + httpServerBaseUri.getPort();
    }

    private static UpstreamProxyDefinition cloudfareProxy(URI httpServerBaseUri) {
        String bypass = toHostAndPort(httpServerBaseUri);
        return ProxyDefinition.builder(FullSocketAddress.define("1.1.1.1", 3128))
                .addProxyBypass(bypass)
                .socks5();
    }

    /**
     * Sets up a traffic collection session with an upstream proxy configured
     * to be bypassed. Makes sure that the intercepting proxy still
     * sees the HTTP interaction.
     * @throws Exception
     */
    @Test
    public void upstreamBypass() throws Exception {
        BypassTestOutcome outcome = testBypass(uri -> Collections.emptyList());
        int numRequestsReceived = outcome.numHttpRequestsReceived;
        List<HttpInteraction> interactions = outcome.httpInteractions;
        assertEquals("num requests", 1, numRequestsReceived);
        assertFalse("expect something intercepted", interactions.isEmpty());
        assertTrue("intercepted HTTP request to our server: " + interactions, interactions.stream().anyMatch(interaction -> {
            return "GET".equals(interaction.request.method)
                    && outcome.httpServerUrl.equals(interaction.request.url);
        }));
    }

    @Test
    public void interceptorBypass() throws Exception {
        BypassTestOutcome outcome = testBypass(uri -> List.of(toHostAndPort(uri)));
        int numRequestsReceived = outcome.numHttpRequestsReceived;
        List<HttpInteraction> interactions = outcome.httpInteractions;
        assertEquals("num requests", 1, numRequestsReceived);
        assertTrue("expect no HTTP request to our server was intercepted: " + interactions, interactions.stream().noneMatch(interaction -> {
            return outcome.httpServerUrl.equals(interaction.request.url);
        }));
    }

    private static class BypassTestOutcome {
        public final List<HttpInteraction> httpInteractions;
        public final int numHttpRequestsReceived;
        public final URI httpServerUrl;

        private BypassTestOutcome(URI httpServerUrl, List<HttpInteraction> httpInteractions, int numHttpRequestsReceived) {
            this.httpServerUrl = httpServerUrl;
            this.httpInteractions = httpInteractions;
            this.numHttpRequestsReceived = numHttpRequestsReceived;
        }
    }

    private BypassTestOutcome testBypass(Function<URI, List<String>> httpServerBaseUriToWebdrivingBypassList) throws Exception {
        AtomicInteger numRequestsReceived = new AtomicInteger(0);
        NanoServer httpServer = NanoServer.builder()
                .getPath("/", s -> {
                    numRequestsReceived.incrementAndGet();
                    return NanoResponse.newFixedLengthResponse("OK");
                })
                .build();
        List<HttpInteraction> interactions = Collections.synchronizedList(new ArrayList<>());
        String pageSource;
        URI httpServerUrl;
        try (NanoControl ctrl = httpServer.startServer()) {
            httpServerUrl = ctrl.baseUri();
            List<String> webdrivingBypasses = httpServerBaseUriToWebdrivingBypassList.apply(httpServerUrl);
            UpstreamProxyDefinition upstreamProxy = cloudfareProxy(httpServerUrl);
            TrafficCollector collector = TrafficCollector.builder(testParameter.createWebDriverFactory(xvfbRule))
                    // This is a Cloudfare address guaranteed not to be hosting an HTTP proxy.
                    // If a client tries to connect through this address, it will fail.
                    // Therefore, if we get through, we know the proxy was bypassed.
                    .upstreamProxy(upstreamProxy, webdrivingBypasses)
                    .build();
            TrafficMonitor monitor = HttpInteraction.monitor(interactions::add);
            pageSource = collector.monitor(webdriver -> {
                System.out.format("GET %s (upstream: %s; webdriving bypass list: %s)%n", httpServerUrl, upstreamProxy, webdrivingBypasses);
                System.out.flush();
                webdriver.get(httpServerUrl.toString());
                return webdriver.getPageSource();
            }, monitor);
        }
        String pageText = Jsoup.parse(pageSource).text();
        assertEquals("page text", "OK", pageText.trim());
        return new BypassTestOutcome(httpServerUrl, interactions, numRequestsReceived.get());
    }

}
