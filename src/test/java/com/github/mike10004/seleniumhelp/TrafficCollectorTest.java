package com.github.mike10004.seleniumhelp;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.littleshoot.proxy.MitmManager;

import javax.net.ssl.SSLEngine;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TrafficCollectorTest {

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

    public static class CollectorWithMonitoringTest {

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
            final List<Pair<ImmutableHttpRequest, ImmutableHttpResponse>> interactions = new ArrayList<>();
            TrafficMonitor monitor = (httpRequest, httpResponse) -> interactions.add(Pair.of(httpRequest, httpResponse));
            collector.collect(driver -> {
                driver.get(url.toString());
                return driver.getPageSource();
            }, monitor);
            System.out.format("interactions:%n%s%n", Joiner.on("\n").join(interactions));
            assertEquals("interactions count", 1, interactions.size());
            ImmutableHttpRequest request = interactions.get(0).getLeft();
            assertEquals("url", url, request.url);
            ImmutableHttpResponse response = interactions.get(0).getRight();
            assertEquals("status", HttpStatus.SC_OK, response.status);

        }
    }

}