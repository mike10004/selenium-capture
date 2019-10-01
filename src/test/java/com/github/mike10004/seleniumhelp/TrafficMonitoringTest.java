package com.github.mike10004.seleniumhelp;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import io.github.mike10004.nitsick.junit.TimeoutRules;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("Convert2Lambda")
public class TrafficMonitoringTest {

    @Rule
    public Timeout timeout = TimeoutRules.from(UnitTests.Settings).getLongRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpWebdriver() {
        UnitTests.setupRecommendedChromeDriver();
    }

    @Test
    public void monitorWithRejectingFilter() throws Exception {
        RejectingFiltersSource rejectingFiltersSource = new RejectingFiltersSource() {
            @Override
            protected boolean isRejectTarget(HttpRequest originalRequest, HttpObject httpObject) {
                System.out.format("isRejectTarget: %s%n", originalRequest.uri());
                return true;
            }
        };
        TrafficCollector collector = TrafficCollector.builder(createWebDriverFactory())
                .filter(rejectingFiltersSource)
                .build();
        RecordingMonitor monitor = new RecordingMonitor();
        String url = "http://checkip.amazonaws.com/";
        collector.monitor(new TrafficGenerator<Void>() {
            @Override
            public Void generate(WebDriver driver) throws IOException {
                System.out.format("visiting %s%n", url);
                driver.get(url);
                System.out.format("visited %s%n", url);
                return null;
            }
        }, monitor);
        assertEquals("filter invocations", 1, rejectingFiltersSource.getRequestCount());
        assertEquals("monitored interactions", 1, monitor.interactions.size());
        assertEquals("expect 500 error", 500, monitor.interactions.get(0).response.status);
    }

    private static WebDriverFactory createWebDriverFactory() {
        return UnitTests.defaultWebDriverFactory();
    }

    @Test
    public void monitoring_http() throws Exception {
        String url = "http://www.example.com/";
        testMonitoring(URI.create(url), TrafficCollector.builder(createWebDriverFactory()).build());
    }

    @Test
    public void monitoring_https() throws Exception {
        File crtFile = File.createTempFile("mitm-certificate", ".pem", temporaryFolder.getRoot());
        TestCertificateAndKeySource.getCertificatePemByteSource().copyTo(Files.asByteSink(crtFile));
        WebDriverFactory wdf = createWebDriverFactory();
        testMonitoring(URI.create("https://www.example.com/"), TrafficCollector.builder(wdf)
                .collectHttps(TestCertificateAndKeySource.create())
                .build());
    }

    private void testMonitoring(URI url, TrafficCollector collector) throws IOException {
        RecordingMonitor monitor = new RecordingMonitor();
        collector.collect(driver -> {
            driver.get(url.toString());
            return driver.getPageSource();
        }, monitor);
        System.out.format("interactions:%n%s%n", Joiner.on("\n").join(monitor.interactions));
        assertEquals("interactions count", 1, monitor.interactions.size());
        HttpInteraction interaction = monitor.interactions.iterator().next();
        assertEquals("url", url, interaction.request.url);
        assertEquals("status", HttpStatus.SC_OK, interaction.response.status);
        List<String> contentTypeValues = interaction.response.getHeaderValues("content-type").collect(Collectors.toList());
        assertEquals("content-type values", 1, contentTypeValues.size());
    }

    public static class DemoRejectingFilter {
        public static void main(String[] args) throws Exception {
            setUpWebdriver();
            RejectingFiltersSource rejectingFiltersSource = new RejectingFiltersSource() {
                @Override
                protected boolean isRejectTarget(HttpRequest originalRequest, HttpObject httpObject) {
                    return true;
                }
            };
            TrafficCollector collector = TrafficCollector.builder(createWebDriverFactory())
                    .filter(rejectingFiltersSource)
                    .build();
            RecordingMonitor monitor = new RecordingMonitor();
            collector.monitor(new TrafficGenerator<Void>() {
                @Override
                public Void generate(WebDriver driver) throws IOException {
                    System.out.println("ready");
                    try {
                        new CountDownLatch(1).await();
                    } catch (InterruptedException ignore) {
                    }
                    return null;
                }
            }, monitor);
        }
    }
}
