package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import io.github.mike10004.nanochamp.server.NanoControl;
import io.github.mike10004.nanochamp.server.NanoResponse;
import io.github.mike10004.nanochamp.server.NanoServer;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.filters.HttpsAwareFiltersAdapter;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ProxyBypassTest {

    private static final boolean SHOW_BROWSER = false;

    private final WebDriverFactory webDriverFactory;

    public ProxyBypassTest(WebDriverFactory webDriverFactory) {
        this.webDriverFactory = webDriverFactory;
    }

    @ClassRule
    public static final XvfbRule xvfbRule = XvfbRule.builder().disabled(SHOW_BROWSER).build();

    @Parameterized.Parameters
    public static List<WebDriverFactory> testCases() {
        Supplier<Map<String, String>> envSupplier = () -> {
            return xvfbRule.getController().newEnvironment();
        };
        //noinspection RedundantArrayCreation
        return Arrays.asList(new WebDriverFactory[]{
                FirefoxWebDriverFactory.builder().environment(envSupplier).build(),
                ChromeWebDriverFactory.builder().environment(envSupplier).build(),
                new JBrowserDriverFactory(),
        });
    }

    /**
     * Exercises a webdriver with an intercepting proxy, confirming that we can configure the webdriver
     * to bypass the proxy for certain addresses.
     * @throws Exception
     */
    @Test
    public void bypassLocalhost() throws Exception {
        System.out.format("bypass: testing with %s%n", webDriverFactory.getClass().getSimpleName());
        prepareWebdriver();
        Map<URI, String> bodyTexts = testBypass(webDriverFactory, host -> true);
        assertEquals("expect to be bypassed", Collections.singleton(MESSAGE_NOT_INTERCEPTED), new HashSet<>(bodyTexts.values()));
    }

    /**
     * Makes sure we're not getting a false positive on the {@link #bypassLocalhost()} test.
     * This exercises the webdriver with an intercepting proxy and no bypasses, and it makes sure that we get only
     * what the proxy serves.
     * @throws Exception
     */
    @Test
    public void nobypass() throws Exception {
        System.out.format("nobypass: testing with %s%n", webDriverFactory.getClass().getSimpleName());
        prepareWebdriver();
        Map<URI, String> bodyTexts = testBypass(webDriverFactory, host -> false);
        assertEquals("expect no bypass", Collections.singleton(MESSAGE_INTERCEPTED), new HashSet<>(bodyTexts.values()));
    }

    private static final String MESSAGE_NOT_INTERCEPTED = "Reached the target server", MESSAGE_INTERCEPTED = "Intercepted by proxy";
    private static final int MAX_BUFFER_SIZE_BYTES = 0; //2 * 1024 * 1024;

    private Map<URI, String> testBypass(WebDriverFactory webDriverFactory, Predicate<? super String> bypassFilter) throws Exception {
        NanoServer server1 = NanoServer.builder()
                .get(whatever -> NanoResponse.status(200).plainTextUtf8(MESSAGE_NOT_INTERCEPTED))
                .build();
        NanoServer server2 = NanoServer.builder()
                .get(whatever -> NanoResponse.status(200).plainTextUtf8(MESSAGE_NOT_INTERCEPTED))
                .build();
        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.addLastHttpFilterFactory(new InterceptingFiltersSource());
        ExecutorService executorService = (Executors.newSingleThreadExecutor());
        proxy.start();
        try {
            String proxyHost = "127.0.0.1";
            int proxyPort = proxy.getPort();
            try (NanoControl ctrl1 = server1.startServer();
                NanoControl ctrl2 = server2.startServer()) {
                HostAndPort targetSocketAddress1 = ctrl1.getSocketAddress();
                HostAndPort targetSocketAddress2 = ctrl2.getSocketAddress();
                List<String> bypasses = Stream.of(targetSocketAddress1, targetSocketAddress2)
                        .map(Object::toString)
                        .filter(bypassFilter)
                        .collect(Collectors.toList());
                WebdrivingConfig config = WebdrivingConfig.builder()
                        .proxy(HostAndPort.fromParts(proxyHost, proxyPort), bypasses)
                        .build();
                URI[] urls = {
                        URI.create(String.format("http://%s/", targetSocketAddress1)),
                        URI.create(String.format("http://%s/", targetSocketAddress2)),
                };
                Map<URI, String> texts = new LinkedHashMap<>();
                try (WebdrivingSession session = webDriverFactory.startWebdriving(config)) {
                    WebDriver driver = session.getWebDriver();
                    for (URI url : urls) {
                        try {
                            Future<String> promise = executorService.submit(new BodyTextFetcher(driver, url));
                            System.out.format("waiting 5 seconds for response...");
                            String value = promise.get(5, TimeUnit.SECONDS);
                            System.out.format("returned \"%s\"%n", value);
                            texts.put(url, value.trim());
                        } catch (java.util.concurrent.TimeoutException e) {
                            System.out.format("no response from %s due to timeout%n", url);
                            texts.put(url, "");
                        }
                    }
                } finally {
                    System.out.format("webdriving session closing%n");
                }
                return texts;
            }
        } finally {
            proxy.stop();
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
        public String call() throws Exception {
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

    private void prepareWebdriver() {
        if (webDriverFactory instanceof FirefoxWebDriverFactory) {
            UnitTests.setupRecommendedGeckoDriver();
        } else if (webDriverFactory instanceof ChromeWebDriverFactory) {
            UnitTests.setupRecommendedChromeDriver();
        } else if (webDriverFactory instanceof JBrowserDriverFactory) {
            //noinspection ConstantConditions
            Assume.assumeFalse("JBrowserDriver does not support proxy host bypasses, even though the ProxyConfig API gives the appearance that it does", webDriverFactory instanceof JBrowserDriverFactory);
        } else {
            throw new AssertionError("unhandled driver factory: " + webDriverFactory);
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
}
