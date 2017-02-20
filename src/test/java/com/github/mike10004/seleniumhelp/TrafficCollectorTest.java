package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import org.junit.Test;
import org.littleshoot.proxy.MitmManager;

import javax.net.ssl.SSLEngine;

import static org.junit.Assert.*;

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

}