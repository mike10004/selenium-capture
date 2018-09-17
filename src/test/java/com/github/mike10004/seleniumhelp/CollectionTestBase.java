package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.lightbody.bmp.core.har.HarContent;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarResponse;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.littleshoot.proxy.ChainedProxyManager;
import org.littleshoot.proxy.ChainedProxyType;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class CollectionTestBase {

    public static final String SYSPROP_TEST_PROXY = "selenium-help.test.proxy.http";

    private static HostAndPort upstreamProxyHostAndPort_ = null;
    private static ChainedProxyType upstreamProxyType_ = null;

    @Rule
    public XvfbRule xvfb = XvfbRule.builder().build();

    @Nullable
    private final HostAndPort upstreamProxyHostAndPort;

    @Nullable
    private final ChainedProxyType upstreamProxyType;

    protected final String protocol;

    protected CollectionTestBase(String protocol) {
        this.protocol = protocol;
        this.upstreamProxyHostAndPort = upstreamProxyHostAndPort_;
        this.upstreamProxyType = upstreamProxyType_;
        checkArgument("http".equals(protocol) || "https".equals(protocol), "protocol must be  http or https: %s", protocol);
    }

    @BeforeClass
    public static void checkClasspath() {
        /*
         * The htmlunit driver depends on an older version of selenium-remote-driver,
         * so this checks that our pom requires the later version to be used.
         */
        String requiredClassname = "org.openqa.selenium.io.CircularOutputStream";
        try {
            Class.forName(requiredClassname);
        } catch (ClassNotFoundException e) {
            fail("pom must declare dependency on newer version of selenium-remote-driver that " +
                    "contains class " + requiredClassname + ", but this class does not exist on " +
                    "the classpath, which probably means that the htmlunit driver's dependency on " +
                    "the older version is taking precedence; revise the pom to require the newer " +
                    "version of selenium-remote-driver");
        }
    }

    @BeforeClass
    public static void setUpstreamProxy() {
        String proxyValue = System.getProperty(SYSPROP_TEST_PROXY);
        if (!Strings.isNullOrEmpty(proxyValue)) {
            if (proxyValue.matches("^\\w+://.*$")) {
                URI proxyUri = URI.create(proxyValue);
                upstreamProxyType_ = ChainedProxyType.valueOf(proxyUri.getScheme().toUpperCase());
                upstreamProxyHostAndPort_ = HostAndPort.fromParts(proxyUri.getHost(), proxyUri.getPort());
            } else {
                upstreamProxyHostAndPort_ = HostAndPort.fromString(proxyValue);
                upstreamProxyType_ = ChainedProxyType.HTTP;
            }
        } else {
            LoggerFactory.getLogger(CollectionTestBase.class).info("this test is much more valuable if you set system or maven property " + SYSPROP_TEST_PROXY + " to an available HTTP proxy that does not have the same external IP address as the JVM's network interface");
        }
    }

    @Before
    public void waitForDisplay() throws InterruptedException {
        xvfb.getController().waitUntilReady();
    }

    /**
     * Gets the port used to build the URL to send a request to. Return -1
     * if you want to use the default port for the protocol.
     * @return the port to use for the URL
     */
    protected int getPort() {
        return -1;
    }

    public static class HttpBinGetResponseData {
        public String url;
        public Map<String, String[]> args = ImmutableMap.of();
        public Map<String, String> headers = ImmutableMap.of();
        public String origin;

        @Override
        public String toString() {
            return "HttpBinGetResponseData{" +
                    "url='" + url + '\'' +
                    ", args=" + args +
                    ", headers=" + headers +
                    ", origin='" + origin + '\'' +
                    '}';
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    protected HarContent testTrafficCollector(WebDriverFactory webDriverFactory) throws IOException {
        int port = getPort();
        System.out.format("testing collector on port %d with %s%n", port, webDriverFactory);
        URL url = new URL(protocol, "httpbin.org", port, "/get?foo=bar&foo=baz");
        HarResponse response = testTrafficCollector(webDriverFactory, url);
        checkState(response.getStatus() == 200, "expected HTTP status 200 but got %s", response.getStatus());
        HarContent content = response.getContent();
        String json = content.getText();
        try {
            Gson gson = new Gson();
            HttpBinGetResponseData responseData = gson.fromJson(json, HttpBinGetResponseData.class);
            checkResponseData(url, responseData);
            return content;
        } catch (JsonParseException e) {
            System.out.format("parse failed on response content %s content-type=\"%s\":%n%n%s%n%n", response.getStatus(), content.getMimeType(), json);
            throw e;
        }
    }

    private static boolean isIpAddressAlreadyResolved(String expectedOrigin) {
        return InetAddressValidator.getInstance().isValid(expectedOrigin);
    }

    protected void checkResponseData(URL url, HttpBinGetResponseData responseData) throws UnknownHostException {
        System.out.format("checking %s%n", responseData);
        assertEquals("url", url.toString(), responseData.url);
        if (upstreamProxyHostAndPort != null) {
            String expectedOrigin = upstreamProxyHostAndPort.getHost();
            // This is not IPv6 compatible
            //noinspection deprecation
            if (!isIpAddressAlreadyResolved(expectedOrigin)) {
                InetAddress ipAddress = InetAddress.getByName(expectedOrigin);
                expectedOrigin = ipAddress.getHostAddress();
            }
            System.out.format("checking origin against proxy %s (%s)%n", upstreamProxyHostAndPort, expectedOrigin);
            assertEquals("origin", expectedOrigin, responseData.origin);
        }
    }

    protected class TestProxySupplier implements java.util.function.Supplier<URI> {

        @Override
        public URI get() {
            if (upstreamProxyHostAndPort == null) {
                return null;
            }
            try {
                return new URIBuilder()
                        .setScheme(ProxyUris.toScheme(upstreamProxyType))
                        .setHost(upstreamProxyHostAndPort.getHost())
                        .setPort(upstreamProxyHostAndPort.getPort())
                        .build();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected HarResponse testTrafficCollector(WebDriverFactory webDriverFactory, final URL url) throws IOException {
        TrafficGenerator<String> urlTrafficGenerator = driver -> {
            driver.get(url.toString());
            String currentUrl = driver.getCurrentUrl(), pageSource = driver.getPageSource();
            String title = StringEscapeUtils.escapeJava(Strings.nullToEmpty(driver.getTitle()));
            System.out.format("%s: '%s' (length=%d)%n", currentUrl, title.isEmpty() ? "[untitled]" : title, pageSource.length());
            return driver.getPageSource();
        };
        HarPlus<String> collection = testTrafficCollector(webDriverFactory, urlTrafficGenerator);
        List<HarEntry> entries = ImmutableList.copyOf(collection.har.getLog().getEntries());
        System.out.format("%d request URLs recorded in HAR:%n", entries.size());
        HarResponse response = null;
        for (int i = 0; i < entries.size(); i++) {
            HarEntry harEntry = entries.get(i);
            String requestUrl = harEntry.getRequest().getUrl();
            System.out.format("  %2d %s%n", i + 1, requestUrl);
            if (url.toString().equals(requestUrl)) {
                checkState(response == null, "response already found matching %s: %s (not sure what to do if two HAR entries match the URL)", url, response);
                response = harEntry.getResponse();
            }
        }
        assertNotNull("no request for " + url + " in HAR", response);
        return response;
    }

    protected <T> HarPlus<T> testTrafficCollector(WebDriverFactory webDriverFactory, TrafficGenerator<T> pageSourceTrafficGenerator) throws IOException {
        TrafficCollectorImpl.Builder tcBuilder = TrafficCollector.builder(webDriverFactory)
                .upstreamProxy(new TestProxySupplier());
        if ("https".equals(protocol)) {
            CertificateAndKeySource certificateAndKeySource = TestCertificateAndKeySource.create();
            tcBuilder.collectHttps(certificateAndKeySource);
        }
        TrafficCollector collector = tcBuilder.build();
        HarPlus<T> collection = collector.collect(pageSourceTrafficGenerator);
        return collection;
    }
}
