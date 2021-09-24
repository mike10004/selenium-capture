package io.github.mike10004.seleniumcapture.testbases;

import com.browserup.bup.mitm.CertificateAndKeySource;
import com.browserup.harreader.model.HarContent;
import com.browserup.harreader.model.HarEntry;
import com.browserup.harreader.model.HarResponse;
import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.common.net.MediaType;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import io.github.mike10004.seleniumcapture.HarPlus;
import io.github.mike10004.seleniumcapture.ProxyDefinition;
import io.github.mike10004.seleniumcapture.TrafficCollector;
import io.github.mike10004.seleniumcapture.TrafficGenerator;
import io.github.mike10004.seleniumcapture.WebDriverFactory;
import io.github.mike10004.seleniumcapture.testing.UnitTests;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public abstract class CollectionTestBase {

    @ClassRule
    public static final XvfbRule xvfb = UnitTests.xvfbRuleBuilder().build();

    @ClassRule
    public static final UpstreamProxyRule upstreamProxyRule = new UpstreamProxyRule();

    @Nullable
    private final HostAndPort upstreamProxyHostAndPort;

    static Map<String, String> createEnvironmentForDisplay(@Nullable String display) {
        Map<String, String> env = new HashMap<>();
        setDisplayEnvironmentVariable(env, display);
        return env;
    }

    static void setDisplayEnvironmentVariable(Map<String, String> env, @Nullable String display) {
        if (display != null) {
            env.put("DISPLAY", display);
        }
    }

    @Before
    public void setUpDriver() {
        webDriverTestParameter.doDriverManagerSetup();
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

    @Before
    public void waitForDisplay() throws InterruptedException {
        xvfb.getController().waitUntilReady();
    }

    protected final WebDriverTestParameter webDriverTestParameter;
    protected final String protocol;

    public CollectionTestBase(WebDriverTestParameter webDriverTestParameter, String protocol) {
        this.webDriverTestParameter = webDriverTestParameter;
        this.protocol = protocol;
        upstreamProxyHostAndPort = upstreamProxyRule.getUpstreamProxyHostAndPort();
        checkArgument("http".equals(protocol) || "https".equals(protocol), "protocol must be  http or https: %s", protocol);
    }

    protected abstract boolean isHeadlessTestDisabled();

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
        public Map<String, ArrayList<String>> args = ImmutableMap.of();
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

    protected Supplier<Map<String, String>> createEnvironmentSupplierForDisplay(boolean headless) {
        if (headless) {
            return HashMap::new;
        } else {
            return () -> {
                String display = xvfb.getController().getDisplay();
                return createEnvironmentForDisplay(display);
            };
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    protected HarContent testTrafficCollectorOnExampleDotCom(WebDriverFactory webDriverFactory) throws IOException {
        // example.com doesn't upgrade insecure requests, so we can use it down test non-https collection
        HarResponse response = testTrafficCollector(webDriverFactory, new URL(protocol, "example.com", getPort(), "/"));
        HarContent content = response.getContent();
        assertEquals("mime type", "text/html", MediaType.parse(content.getMimeType()).withoutParameters().toString());
        return content;
    }

    @SuppressWarnings("UnusedReturnValue")
    protected HarContent testTrafficCollectorOnHttpbin(WebDriverFactory webDriverFactory) throws IOException {
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
            //noinspection
            if (!isIpAddressAlreadyResolved(expectedOrigin)) {
                InetAddress ipAddress = InetAddress.getByName(expectedOrigin);
                expectedOrigin = ipAddress.getHostAddress();
            }
            System.out.format("checking origin against proxy %s (%s)%n", upstreamProxyHostAndPort, expectedOrigin);
            assertEquals("origin", expectedOrigin, responseData.origin);
        }
    }


    protected HarResponse testTrafficCollector(WebDriverFactory webDriverFactory, final URL url) throws IOException {
        TrafficGenerator<String> urlTrafficGenerator = driver -> {
            driver.get(url.toString());
            String currentUrl = driver.getCurrentUrl(), pageSource = driver.getPageSource();
            String title = StringEscapeUtils.escapeJava(Strings.nullToEmpty(driver.getTitle()));
            System.out.format("%s: '%s' (length=%d)%n%n%s%n%n", currentUrl, title.isEmpty() ? "[untitled]" : title, pageSource.length(), pageSource);
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
        return testTrafficCollector(webDriverFactory, pageSourceTrafficGenerator, protocol, upstreamProxyRule::getProxySpecificationUri);
    }

    public static <T> HarPlus<T> testTrafficCollector(WebDriverFactory webDriverFactory, TrafficGenerator<T> pageSourceTrafficGenerator, String protocol, Supplier<URI> proxySpecUriSupplier) throws IOException {
        TrafficCollector.Builder tcBuilder = TrafficCollector.builder(webDriverFactory);
        URI upstreamProxy = proxySpecUriSupplier.get();
        if (upstreamProxy != null) {
            tcBuilder.upstreamProxy(ProxyDefinition.fromUri(upstreamProxy), Collections.emptyList());
        }
        if ("https".equals(protocol)) {
            CertificateAndKeySource certificateAndKeySource = TestCertificateAndKeySource.create();
            tcBuilder.collectHttps(certificateAndKeySource);
        }
        TrafficCollector collector = tcBuilder.build();
        HarPlus<T> collection = collector.collect(pageSourceTrafficGenerator);
        return collection;
    }
}
