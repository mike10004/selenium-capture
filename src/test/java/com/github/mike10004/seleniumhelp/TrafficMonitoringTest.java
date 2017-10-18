package com.github.mike10004.seleniumhelp;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("Convert2Lambda")
public class TrafficMonitoringTest {

    @Test
    public void monitorWithRejectingFilter() throws Exception {
        RejectingFiltersSource rejectingFiltersSource = new RejectingFiltersSource() {
            @Override
            protected boolean isRejectTarget(HttpRequest originalRequest, HttpObject httpObject) {
                return true;
            }
        };
        TrafficCollector collector = TrafficCollector.builder(new JBrowserDriverFactory())
                .filter(rejectingFiltersSource)
                .build();
        RecordingMonitor monitor = new RecordingMonitor();
        collector.monitor(new TrafficGenerator<Void>() {
            @Override
            public Void generate(WebDriver driver) throws IOException {
                driver.get("http://checkip.amazonaws.com/");
                return null;
            }
        }, monitor);
        assertEquals("filter invocations", 1, rejectingFiltersSource.getRequestCount());
        assertEquals("monitored interactions", 1, monitor.interactions.size());
        monitor.interactions.forEach(interaction -> {
            assertEquals("expect 500 error", 500, interaction.response.status);
        });
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void monitoring_http() throws Exception {
        String url = "http://www.example.com/";
        testMonitoring(URI.create(url), TrafficCollector.builder(new JBrowserDriverFactory()).build());
    }

    @Test
    public void monitoring_https() throws Exception {
        File crtFile = File.createTempFile("mitm-certificate", ".pem", temporaryFolder.getRoot());
        TestCertificateAndKeySource.getCertificatePemByteSource().copyTo(Files.asByteSink(crtFile));
        WebDriverFactory wdf = new JBrowserDriverFactory(crtFile);
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

    }
}
