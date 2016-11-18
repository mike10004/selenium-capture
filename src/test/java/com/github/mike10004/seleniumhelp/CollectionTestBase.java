package com.github.mike10004.seleniumhelp;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.google.gson.Gson;
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
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CollectionTestBase {

    protected final String protocol;

    @BeforeClass
    public static void configureLogging() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
    }

    protected CollectionTestBase(String protocol) {
        this.protocol = protocol;
        checkArgument("http".equals(protocol) || "https".equals(protocol), "protocol must be  http or https: %s", protocol);
    }

    protected int getPort() {
        return "http".equals(protocol) ? 80 : 443;
    }

    public static class HttpBinGetResponseData {
        public Map<String, String[]> args = ImmutableMap.of();
        public Map<String, String> headers = ImmutableMap.of();
        public String origin;
        public String url;
    }

    protected HarContent testTrafficCollector(WebDriverFactory webDriverFactory) throws IOException {
        int port = getPort();
        System.out.format("testing collector on port %d with %s%n", port, webDriverFactory);
        URL url = new URL(protocol, "httpbin.org", port, "/get?foo=bar&foo=baz");
        HarContent content = testTrafficCollector(webDriverFactory, url);
        String json = content.getText();
        Gson gson = new Gson();
        HttpBinGetResponseData responseData = gson.fromJson(json, HttpBinGetResponseData.class);
        assertEquals("url", url.toString(), responseData.url);
        return content;
    }

    protected void checkResponseData(HttpBinGetResponseData responseData) {

    }

    @SuppressWarnings("Duplicates")
    protected HarContent testTrafficCollector(WebDriverFactory webDriverFactory, final URL url) throws IOException {
        CertificateAndKeySource certificateAndKeySource = new TestCertificateAndKeySource();
        TrafficCollector collector = new TrafficCollector(webDriverFactory, certificateAndKeySource, AnonymizingFiltersSource.getInstance());
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
