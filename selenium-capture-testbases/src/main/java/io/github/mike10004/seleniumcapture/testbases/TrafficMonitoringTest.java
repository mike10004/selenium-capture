package io.github.mike10004.seleniumcapture.testbases;

import io.github.mike10004.seleniumcapture.TrafficCollector;
import io.github.mike10004.seleniumcapture.TrafficGenerator;
import io.github.mike10004.seleniumcapture.WebDriverFactory;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import io.github.bonigarcia.wdm.config.DriverManagerType;
import io.github.mike10004.nitsick.junit.TimeoutRules;
import io.github.mike10004.seleniumcapture.testing.UnitTests;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@SuppressWarnings({"Convert2Lambda", "HttpUrlsUsage"})
public abstract class TrafficMonitoringTest {

    @Rule
    public Timeout timeout = TimeoutRules.from(UnitTests.Settings).getLongRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final DriverManagerType driverManagerType;

    public TrafficMonitoringTest(DriverManagerType driverManagerType) {
        this.driverManagerType = driverManagerType;
    }

    @Before
    public void setUpWebdriver() {
        TestBases.setupWebDriver(driverManagerType);
    }

    @Test
    public void monitorWithRejectingFilter() throws Exception {
        RejectingFiltersSource rejectingFiltersSource = new RejectingFiltersSource() {
            @Override
            protected boolean isRejectTarget(HttpRequest originalRequest, String fullUrl, HttpObject httpObject) {
                System.out.format("isRejectTarget: %s%n", fullUrl);
                return true;
            }
        };
        TrafficCollector collector = TrafficCollector.builder(createWebDriverFactory(false))
                .filter(rejectingFiltersSource)
                .build();
        RecordingMonitor monitor = new RecordingMonitor();
        String url = "http://checkip.amazonaws.com/";
        collector.monitor(new TrafficGenerator<Void>() {
            @Override
            public Void generate(WebDriver driver) {
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

    protected abstract WebDriverFactory createWebDriverFactory(boolean acceptInsecureCerts);

    @Test
    public void monitoring_http() throws Exception {
        String url = "http://www.example.com/";
        testMonitoring(URI.create(url), TrafficCollector.builder(createWebDriverFactory(false)).build());
    }

    @Test
    public void monitoring_https() throws Exception {
        File crtFile = File.createTempFile("mitm-certificate", ".pem", temporaryFolder.getRoot());
        TestCertificateAndKeySource.getCertificatePemByteSource().copyTo(Files.asByteSink(crtFile));
        WebDriverFactory wdf = createWebDriverFactory(true);
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
        // We disable the favicon request in UnitTests.createFirefoxPreferences();
        // Otherwise there would be at least one more request here. If this test is
        // modified to use Chrome/Chromium instead, it will likely have to account for
        // the many automatic requests that browser makes.
        assertEquals("interactions count", 1, monitor.interactions.size());
        HttpInteraction interaction = monitor.interactions.iterator().next();
        assertEquals("url", url, interaction.request.url);
        assertEquals("status", HttpStatus.SC_OK, interaction.response.status);
        List<String> contentTypeValues = interaction.response.getHeaderValues("content-type").collect(Collectors.toList());
        assertEquals("content-type values", 1, contentTypeValues.size());
    }

}
