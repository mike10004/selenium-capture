package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedInts;
import io.github.mike10004.nanochamp.server.NanoControl;
import io.github.mike10004.nanochamp.server.NanoResponse;
import io.github.mike10004.nanochamp.server.NanoServer;
import io.github.mike10004.nitsick.junit.TimeoutRules;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import com.browserup.harreader.model.HarEntry;
import com.browserup.harreader.model.HarRequest;
import com.browserup.bup.mitm.CertificateAndKeySource;
import com.browserup.bup.mitm.manager.ImpersonatingMitmManager;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.littleshoot.proxy.MitmManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import javax.net.ssl.SSLEngine;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TrafficCollectorTest {

    @Rule
    public Timeout timeout = TimeoutRules.from(UnitTests.Settings).getLongRule();

    @Test
    public void createMitmManager() throws Exception {
        CertificateAndKeySource certificateAndKeySource = TestCertificateAndKeySource.create();
        MitmManager mitmManager = ImpersonatingMitmManager.builder()
                .rootCertificateSource(certificateAndKeySource)
                .build();
        SSLEngine sslEngine = mitmManager.serverSslEngine();
        System.out.format("sslEngine=%s%n", sslEngine);
        assertNotNull(sslEngine);
    }

    @Test
    public void collectHarWithFilterCausingServerErrors() throws Exception {
        Random random = new Random(getClass().getName().hashCode());
        List<String> rejectionTexts = new ArrayList<>();
        Set<String> rejectTargets = Collections.singleton("http://icanhazip.com/");
        RejectingFiltersSource rejectingFiltersSource = new RejectingFiltersSource() {
            @Override
            protected boolean isRejectTarget(HttpRequest originalRequest, String fullUrl, HttpObject httpObject) {
                System.out.format("checking whether to reject %s (%08x %s@%08x)%n", fullUrl, System.identityHashCode(originalRequest), getHttpObjectClass(httpObject), System.identityHashCode(httpObject));
                return rejectTargets.contains(fullUrl);
            }

            @Override
            protected String constructRejectionText(HttpRequest originalRequest, String fullUrl, HttpObject httpObject) {
                String text = UnsignedInts.toString(random.nextInt());
                rejectionTexts.add(text);
                return text;
            }
        };
        TrafficCollector collector = TrafficCollector.builder(UnitTests.headlessWebDriverFactory())
                .filter(rejectingFiltersSource)
                .build();
        Set<String> nonRejectTargets = ImmutableSet.of("http://checkip.amazonaws.com/");
        List<String> urlTargets = ImmutableList.copyOf(Iterables.concat(nonRejectTargets, rejectTargets));
        HarPlus<List<String>> collection = collector.collect(new TrafficGenerator<List<String>>() {
            @Override
            public List<String> generate(WebDriver driver) {
                return urlTargets.stream().map(url -> {
                    driver.get(url);
                    return driver.findElement(By.tagName("body")).getText().trim();
                }).collect(Collectors.toList());
            }
        });
        assertEquals("filter invocations", urlTargets.size(), rejectingFiltersSource.getRequestCount());
        System.out.format("results: %s%n", collection.result);
        assertTrue("all rejection texts in results", collection.result.containsAll(rejectionTexts));
        List<HarEntry> entries = collection.har.getLog().getEntries();
        /*
         * Confirm that rejected requests never make it into the HAR. This is a "feature"
         * of the HarCaptureFilter, I suppose. The filter doesn't override proxyToClientResponse,
         * so if one of your filters skips sending the response from proxy to remote server, then
         * the HAR will not contain that interaction, not even a response-less request.
         */
        Set<String> actualHarRequestUrls = entries.stream()
                .map(HarEntry::getRequest)
                .map(HarRequest::getUrl)
                .collect(Collectors.toSet());
        Set<String> violations = Sets.intersection(actualHarRequestUrls, rejectTargets);
        assertTrue("violations should be empty: " + violations, violations.isEmpty());
    }

    private final static ImmutableList<Class<?>> potentialClasses = ImmutableList.<Class<?>>builder()
            .add(DefaultFullHttpRequest.class)
            .add(FullHttpRequest.class)
            .add(HttpRequest.class)
            .build();

    private String getHttpObjectClass(HttpObject object) {
        for (Class<?> potential : potentialClasses) {
            if (potential.isInstance(object)) {
                return potential.getSimpleName();
            }
        }
        return object == null ? null : object.getClass().getName();
    }

    @Test
    public void drive() throws Exception {
        String expected = "hello";
        NanoServer nano = NanoServer.builder().getPath("/hello", session -> NanoResponse.status(200).plainTextUtf8(expected)).build();
        String bodyText;
        try (NanoControl ctrl = nano.startServer()) {
            TrafficCollector collector = TrafficCollector.builder(UnitTests.headlessWebDriverFactory()).build();
            bodyText = collector.drive(driver -> {
                try {
                    driver.get(new URIBuilder(ctrl.baseUri()).setPath("/hello").build().toString());
                    return driver.findElement(By.tagName("body")).getText();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        assertEquals("body text", expected, bodyText);
    }

}