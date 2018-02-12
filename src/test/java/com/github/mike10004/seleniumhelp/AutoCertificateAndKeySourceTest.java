package com.github.mike10004.seleniumhelp;

import com.github.mike10004.seleniumhelp.AutoCertificateAndKeySource.SerializableForm;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AutoCertificateAndKeySourceTest {

    private static final String SYSPROP_OPENSSL_EXECUTABLE = "openssl.executable.path";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected ExecutableConfig makeOpensslConfig() {
        String path = Strings.emptyToNull(System.getProperty(SYSPROP_OPENSSL_EXECUTABLE));
        if (path != null) {
            File file = new File(path);
            System.out.format("using openssl executable at %s%n", file);
            return ExecutableConfig.byPathOnly(file);
        }
        return ExecutableConfig.byNameOnly("openssl");
    }

    protected ExecutableConfig makeKeytoolConfig() {
        return ExecutableConfig.byNameOnly("keytool");
    }

    @Test
    public void generateAndUseCertificate() throws Exception {
        File scratchDir = temporaryFolder.getRoot();
        testCertificateUsage(scratchDir.toPath());
    }

    @Test
    public void invokeLoadTwice() throws Exception {
        Path scratchDir = temporaryFolder.getRoot().toPath();
        try (CountingAutoCertificateAndKeySource certificateAndKeySource = new CountingAutoCertificateAndKeySource(scratchDir, random)) {
            certificateAndKeySource.load();
            certificateAndKeySource.load();
            assertEquals("num generate invocations", 1, certificateAndKeySource.generateInvocations.get());
        }
    }

    private static Random random = new Random(AutoCertificateAndKeySourceTest.class.getName().hashCode());

    private File createTempPathname(Path scratchDir, String suffix) throws IOException {
        byte[] bytes = new byte[16];
        String name = BaseEncoding.base32().encode(bytes) + suffix;
        File file = scratchDir.resolve(name).toFile();
        if (file.isFile()) {
            throw new IOException("file already exists: " + file);
        }
        return file;
    }

    private static class CountingAutoCertificateAndKeySource extends AutoCertificateAndKeySource {

        private final AtomicInteger generateInvocations = new AtomicInteger();

        public CountingAutoCertificateAndKeySource(Path scratchDir, Random random) {
            super(scratchDir, random);
        }

        @Override
        protected MemoryKeyStoreCertificateSource generate(String keystorePassword) throws IOException {
            generateInvocations.incrementAndGet();
            return super.generate(keystorePassword);
        }
    }

    private void testCertificateUsage(Path scratchDir) throws IOException, InterruptedException {
        try (CountingAutoCertificateAndKeySource certificateAndKeySource = new CountingAutoCertificateAndKeySource(scratchDir, random)) {
            SerializableForm serializableForm = certificateAndKeySource.createSerializableForm();
            File keystoreFile = createTempPathname(scratchDir, ".keystore");
            File pkcs12File = createTempPathname(scratchDir, ".p12");
            certificateAndKeySource.createPKCS12File(makeKeytoolConfig(), pkcs12File);
            File pemFile = File.createTempFile("certificate", ".pem", scratchDir.toFile());
            certificateAndKeySource.createPemFile(makeOpensslConfig(), pkcs12File, pemFile);
            Files.write(Base64.getDecoder().decode(serializableForm.keystoreBase64), keystoreFile);
            TrafficCollector collector = TrafficCollector.builder(new JBrowserDriverFactory(pemFile))
                    .collectHttps(certificateAndKeySource)
                    .build();
            String url = "https://example.com/";
            HarPlus<String> harPlusContent = collector.collect(driver -> {
                driver.get(url);
                return driver.getPageSource();
            });
            HarResponse response = harPlusContent.har.getLog().getEntries().stream()
                    .filter(entry -> url.equals(entry.getRequest().getUrl()))
                    .map(HarEntry::getResponse)
                    .findFirst()
                    .orElse(null);
            assertNotNull("response in har", response);
            assertEquals("response status", 200, response.getStatus());
            assertEquals("num generate() invocations", 1, certificateAndKeySource.generateInvocations.get());
        }

    }
}