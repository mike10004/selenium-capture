package com.github.mike10004.seleniumhelp;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

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
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkState;

/**
 * Example of using and capturing a newly generated certificate.
 * The password is printed to the console and the captured keystore file
 * is in the temporary directory created at the beginning. *
 */
public class GenerateNewCertificate {

    // https://github.com/lightbody/browsermob-proxy/blob/master/mitm/src/test/java/net/lightbody/bmp/mitm/example/SaveGeneratedCAExample.java
    public static void main(String[] args) throws Exception {
        Path systemTempDir = new File(System.getProperty("java.io.tmpdir")).toPath();
        Path scratchDir = java.nio.file.Files.createTempDirectory(systemTempDir, "GenerateNewCertificate");
        try (CapturingAutoCertificateAndKeySource rootCertificateGenerator = new CapturingAutoCertificateAndKeySource(scratchDir)) {
            rootCertificateGenerator.load();
            File keystoreFile = rootCertificateGenerator.getKeystoreFile();
            checkState(keystoreFile != null, "keystore file not generated");
            String keystorePassword = rootCertificateGenerator.getKeystorePassword();
            System.out.format("generated keystore password: %s%n", keystorePassword);
            checkState(keystorePassword != null, "keystore password not generated");
            useCertificate(rootCertificateGenerator, keystoreFile, keystorePassword);
        }
        Collection<File> files = FileUtils.listFiles(scratchDir.toFile(), null, true);
        files.forEach(file -> {
            System.out.format("%.1fK %s%n", file.length() / 1024f, file);
        });
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
        protected void keystorePasswordGenerated(String password) {
            keystorePasswordRef.set(password);
        }

        @Override
        protected void keystoreBytesGenerated(ByteSource byteSource) {
            try {
                File outputFile = File.createTempFile("captured", ".keystore", scratchDir.toFile());
                byteSource.copyTo(Files.asByteSink(outputFile));
                keystoreFileRef.set(outputFile);
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
        // changed: don't provide a TrustStrategy because we only want to trust the self-signed cert we specify
        SSLContext customSslContext = SSLContexts.custom()
                .loadTrustMaterial(keystoreFile, keystorePassword.toCharArray(), null)
                .build();
        String HEADER_NAME = "X-This-Was-Sent-Through-The-Proxy";
        String headerValue = UUID.randomUUID().toString();
        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.setMitmManager(mitmManager);
        proxy.addLastHttpFilterFactory(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {
                return new HttpFiltersAdapter(originalRequest) {
                    @Override
                    public io.netty.handler.codec.http.HttpResponse proxyToServerRequest(HttpObject httpObject) {
                        if (httpObject instanceof HttpRequest) {
                            HttpRequest request = (HttpRequest) httpObject;
                            HttpHeaders headers = request.headers();
                            headers.add(HEADER_NAME, headerValue);
                        }
                        return null;
                    }
                };
            }
        });
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
            checkState(responseText != null, "response text null");
            JsonObject responseObj = new JsonParser().parse(responseText).getAsJsonObject();
            JsonElement headers = responseObj.get("headers");
            checkState(headers != null, "headers not present in %s", responseObj);
            JsonElement customHeaderEl = headers.getAsJsonObject().get(HEADER_NAME);
            checkState(customHeaderEl != null, "%s header not present in %s", HEADER_NAME, headers);
            String customHeaderValue = customHeaderEl.getAsString();
            checkState(headerValue.equals(customHeaderValue), "'%s' header should specify that browsermobproxy was used, but its value is %s", HEADER_NAME, customHeaderValue);
        } finally {
            proxy.stop();
        }
    }
}
