package com.github.mike10004.seleniumhelp;

import com.github.mike10004.seleniumhelp.AutoCertificateAndKeySource.SerializableForm;
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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AutoCertificateAndKeySourceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void generateAndUseCertificate() throws Exception {
        File scratchDir = temporaryFolder.getRoot();
        testCertificateUsage(scratchDir.toPath());
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

    private void testCertificateUsage(Path scratchDir) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        try (AutoCertificateAndKeySource certificateAndKeySource = new AutoCertificateAndKeySource(scratchDir, random)) {
            SerializableForm serializableForm = certificateAndKeySource.createSerializableForm();
            File keystoreFile = createTempPathname(scratchDir, ".keystore");
            File pkcs12File = createTempPathname(scratchDir, ".p12");
            certificateAndKeySource.createPKCS12File(pkcs12File);
            File pemFile = File.createTempFile("certificate", ".pem", scratchDir.toFile());
            certificateAndKeySource.createPemFile(pkcs12File, pemFile);
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
        }
    }
}