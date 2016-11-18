package com.github.mike10004.seleniumhelp;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarContent;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.TrustSource;
import net.lightbody.bmp.mitm.exception.CertificateSourceException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringEscapeUtils;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.openqa.selenium.WebDriver;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarContent;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.TrustSource;
import net.lightbody.bmp.mitm.exception.CertificateSourceException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.openqa.selenium.WebDriver;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class WireMockCollectorTestBase {

    protected final String protocol;

    @BeforeClass
    public static void configureLogging() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
    }

    @Rule
    public WireMockRule wireMockRule;

    protected WireMockCollectorTestBase(String protocol) {
        this.protocol = protocol;
        checkArgument("http".equals(protocol) || "https".equals(protocol), "protocol must be  http or https: %s", protocol);
        wireMockRule = new WireMockRule(buildWireMockConfig(protocol));
    }

    protected WireMockConfiguration buildWireMockConfig(String protocol) {
        WireMockConfiguration c = WireMockConfiguration.wireMockConfig();
        if ("http".equals(protocol)) {
            c.dynamicPort();
        } else if ("https".equals(protocol)) {
            c.dynamicHttpsPort();
        } else {
            throw new IllegalArgumentException("protocol must be http or https: " + protocol);
        }
        return c;
    }

    protected byte[] loadTestImage() throws IOException {
        return Resources.toByteArray(getClass().getResource("/rose.jpg"));
    }

    protected MediaType getTestImageType() {
        return MediaType.JPEG;
    }

    protected static final String TEST_PATH_TEXT = "/text", TEST_PATH_IMAGE = "/image";

    protected void prepareMockServer(String text, byte[] imageBytes, MediaType imageType) {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(TEST_PATH_TEXT))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
                        .withBody(text)));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(TEST_PATH_IMAGE))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, imageType.toString())
                        .withBody(imageBytes)));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/favicon.ico"))
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.SC_NOT_FOUND).withBody(new byte[0])));
    }

    protected int getPort() {
        return "http".equals(protocol) ? wireMockRule.port() : wireMockRule.httpsPort();
    }

    protected void testTrafficCollector(WebDriverFactory webDriverFactory) throws IOException {
        byte[] imageBytes = loadTestImage();
        String testText = "hello, world";
        prepareMockServer(testText, imageBytes, getTestImageType());
        String actualText = testTrafficCollector(webDriverFactory, TEST_PATH_TEXT).getText();
        assertEquals("text", testText, actualText);
        String actualImageBytesBase64 = testTrafficCollector(webDriverFactory, TEST_PATH_IMAGE).getText();
        byte[] actualImageBytes = new Base64().decode(actualImageBytesBase64);
        assertArrayEquals("imageBytes", imageBytes, actualImageBytes);
    }

    protected HarContent testTrafficCollector(WebDriverFactory webDriverFactory, String path) throws IOException {
        int port = getPort();
        System.out.format("testing collector on port %d with %s%n", port, webDriverFactory);
        HarContent content = testTrafficCollector(webDriverFactory, new URL(protocol, "localhost", port, path));
        return content;
    }

    protected TrustSource buildBmpTrustSource() {
        URL wiremockKeystore = Resources.getResource("keystore");
        System.out.format("wiremock keystore: %s%n", wiremockKeystore);
        byte[] keyStoreBytes;
        try {
            keyStoreBytes = Resources.asByteSource(wiremockKeystore).read();
        } catch (IOException e) {
            throw new CertificateSourceException("Unable to open KeyStore byte source: " + wiremockKeystore, e);
        }
        char[] keyStorePasswordChars = "password".toCharArray();
        String keyStoreType = "JKS";
        KeyStore keyStore = new MemorySecurityProviderTool().loadKeyStore(keyStoreBytes, keyStoreType, keyStorePasswordChars);
        Arrays.fill(keyStorePasswordChars, '\0');
        Arrays.fill(keyStoreBytes, (byte) 0);
        TrustSource trustSource = TrustSource.defaultTrustSource().add(keyStore);
        trustSource = TrustSource.empty();
        return trustSource;
    }

    private static class FunctionFiltersSource extends HttpFiltersSourceAdapter {

        private final Function<HttpRequest, HttpResponse> responseProvider;

        public FunctionFiltersSource(final Predicate<HttpRequest> uriPredicate, final HttpResponseStatus responseStatus) {
            this(new Function<HttpRequest, HttpResponse>() {
                @Nullable
                @Override
                public HttpResponse apply(HttpRequest input) {
                    HttpResponse response = null;
                    if (uriPredicate.apply(input)) {
                        response = new DefaultHttpResponse(input.getProtocolVersion(), responseStatus);
                    }
                    return response;
                }
            });
        }

        public FunctionFiltersSource(Function<HttpRequest, HttpResponse> responseProvider) {
            this.responseProvider = responseProvider;
        }

        @Override
        public HttpFilters filterRequest(HttpRequest originalRequest) {
            return new HttpFiltersAdapter(originalRequest) {
                @Override
                public HttpResponse proxyToServerRequest(HttpObject httpObject) {
                    if (httpObject instanceof HttpRequest) {
                        HttpRequest req = (HttpRequest) httpObject;
                        HttpResponse response = responseProvider.apply(req);
                        return response;
                    }
                    return null;
                }
            };
        }
    }

    private static Function<HttpRequest, HttpResponse> responseForMatchingUris(final Predicate<URI> uriPredicate, HttpResponseStatus status) {
        return new Function<HttpRequest, HttpResponse>() {
            @Nullable
            @Override
            public HttpResponse apply(HttpRequest input) {
                if (uriPredicate.apply(URI.create(input.getUri()))) {
                    return new DefaultHttpResponse(input.getProtocolVersion(), status);
                }
                return null;
            }
        };
    }

    protected HarContent testTrafficCollector(WebDriverFactory webDriverFactory, final URL url) throws IOException {
        CertificateAndKeySource certificateAndKeySource = new TestCertificateAndKeySource();
        TrafficCollector collector = new TrafficCollector(webDriverFactory, certificateAndKeySource, AnonymizingFiltersSource.getInstance()) {
            @Override
            protected BrowserMobProxy createProxy(CertificateAndKeySource certificateAndKeySource) {
                BrowserMobProxy proxy = super.createProxy(certificateAndKeySource);
//                proxy.setTrustAllServers(true);
//                checkState(proxy.getChainedProxy() == null, "chained proxy expected null here: %s", proxy.getChainedProxy());
//                X509Certificate cert = certificateAndKeySource.load().getCertificate();
//                proxy.setTrustSource(TrustSource.defaultTrustSource().add(cert));

//                proxy.setTrustSource(buildBmpTrustSource());
                proxy.addFirstHttpFilterFactory(new FunctionFiltersSource(responseForMatchingUris(new Predicate<URI>() {
                    @Override
                    public boolean apply(@Nullable URI input) {
                        return input != null && input.getHost() != null && input.getHost().endsWith(".mozilla.net");
                    }
                }, HttpResponseStatus.INTERNAL_SERVER_ERROR)));
                return proxy;
            }
        };
        final AtomicReference<String> pageSourceRef = new AtomicReference<>();
        Har har = collector.collect(new TrafficGenerator() {
            @Override
            public void generate(WebDriver driver) throws IOException {
                driver.get(url.toString());
                String currentUrl = driver.getCurrentUrl(), pageSource = driver.getPageSource(), title = driver.getTitle();
                System.out.format("%s: '%s' (length=%d)%n", currentUrl, StringEscapeUtils.escapeJava(title), pageSource.length());
                pageSourceRef.set(driver.getPageSource());
            }
        });
        List<HarEntry> entries = ImmutableList.copyOf(har.getLog().getEntries());
        System.out.format("%d request URLs recorded in HAR:%n", entries.size());
        for (int i = 0; i < entries.size(); i++) {
            HarEntry harEntry = entries.get(i);
            String requestUrl = harEntry.getRequest().getUrl();
            System.out.format("  %2d %s%n", i + 1, requestUrl);
            if (url.toString().equals(requestUrl)) {
                return harEntry.getResponse().getContent();
            }
        }
        Assert.fail("no request for " + url + " in HAR");
        throw new IllegalStateException();
    }
}
