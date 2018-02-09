package com.github.mike10004.seleniumhelp;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.gson.JsonParser;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkState;

public class GenerateNewCertificate {

    // https://github.com/lightbody/browsermob-proxy/blob/master/mitm/src/test/java/net/lightbody/bmp/mitm/example/SaveGeneratedCAExample.java
    public static void main(String[] args) throws Exception {
        Path scratchDir = new File(System.getProperty("java.io.tmpdir")).toPath();
        try (CapturingAutoCertificateAndKeySource rootCertificateGenerator = new CapturingAutoCertificateAndKeySource(scratchDir)) {
            rootCertificateGenerator.load();
            File keystoreFile = rootCertificateGenerator.getKeystoreFile();
            checkState(keystoreFile != null, "keystore file not generated");
            String keystorePassword = rootCertificateGenerator.getKeystorePassword();
            checkState(keystorePassword != null, "keystore password not generated");
            useCertificate(rootCertificateGenerator, keystoreFile, keystorePassword);
        }
    }

    private static class CapturingAutoCertificateAndKeySource extends AutoCertificateAndKeySource {
        private final Path scratchDir;
        private final AtomicReference<File> keystoreFileRef;
        private final AtomicReference<String> keystorePasswordRef;

        public CapturingAutoCertificateAndKeySource(Path scratchDir) {
            super(scratchDir);
            this.scratchDir = scratchDir;
            keystoreFileRef = new AtomicReference<>();
            keystorePasswordRef = new AtomicReference<>();
        }

        @Override
        protected void passwordGenerated(String password) {
            keystorePasswordRef.set(password);
        }

        @Override
        protected void keystoreBytesGenerated(ByteSource byteSource) {
            try {
                File outputFile = File.createTempFile("captured", ".keystore", scratchDir.toFile());
                byteSource.copyTo(Files.asByteSink(outputFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Nullable
        public String getKeystorePassword() {
            return keystorePasswordRef.get();
        }

        @Nullable
        public File getKeystoreFile() {
            return keystoreFileRef.get();
        }
    }

    @SuppressWarnings("StaticPseudoFunctionalStyleMethod")
    private static void useCertificate(CertificateAndKeySource rootCertificateGenerator, File keystoreFile, String keystorePassword) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
                .rootCertificateSource(rootCertificateGenerator)
                .build();
        SSLContext customSslContext = SSLContexts.custom()
                .loadTrustMaterial(keystoreFile, keystorePassword.toCharArray(), new TrustSelfSignedStrategy())
                .build();
        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.setMitmManager(mitmManager);
        proxy.start();
        try (CloseableHttpClient client = HttpClients.custom()
                .setSSLContext(customSslContext)
                .setProxy(new HttpHost("localhost", proxy.getPort()))
                .build()) {
            System.out.println("sending request...");
            String responseText;
            try (CloseableHttpResponse response = client.execute(new HttpGet(URI.create("https://httpbin.org/get")))) {
                responseText = EntityUtils.toString(response.getEntity());
                System.out.format("response headers: %s%n", Iterables.transform(Arrays.asList(response.getAllHeaders()), input -> String.format("%s=%s", input.getName(), input.getValue())));
            }
            System.out.println(responseText);
            String viaHeader = new JsonParser().parse(responseText).getAsJsonObject().get("headers").getAsJsonObject().get("Via").getAsString();
            checkState("1.1 browsermobproxy".equals(viaHeader), "'Via' header should specify that browsermobproxy was used, but its value is %s", viaHeader);
        } finally {
            proxy.stop();
        }
    }
}
