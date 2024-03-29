package io.github.mike10004.seleniumcapture.testbases;

import io.github.mike10004.seleniumcapture.AutoCertificateAndKeySource;
import io.github.mike10004.seleniumcapture.AutoCertificateAndKeySource.SerializableForm;
import io.github.mike10004.seleniumcapture.HarPlus;
import io.github.mike10004.seleniumcapture.KeystoreFileCreator;
import io.github.mike10004.seleniumcapture.KeystoreInput;
import io.github.mike10004.seleniumcapture.OpensslKeystoreFileCreator;
import io.github.mike10004.seleniumcapture.TrafficCollector;
import io.github.mike10004.seleniumcapture.WebDriverFactory;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import com.browserup.harreader.model.HarEntry;
import com.browserup.harreader.model.HarResponse;
import io.github.mike10004.seleniumcapture.testing.UnitTests;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class AutoCertificateAndKeySourceTestBase {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected abstract void setupWebDriver();

    protected abstract WebDriverFactory buildHeadlessFactory();

    @Test
    public void generateAndUseCertificate() throws Exception {
        File scratchDir = temporaryFolder.getRoot();
        testCertificateUsage(scratchDir.toPath());
    }

    private static final Random random = new Random(AutoCertificateAndKeySourceTestBase.class.getName().hashCode());

    private File createTempPathname(Path scratchDir, String suffix) throws IOException {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String name = BaseEncoding.base16().encode(bytes) + suffix;
        File file = scratchDir.resolve(name).toFile();
        if (file.isFile()) {
            throw new IOException("file already exists: " + file);
        }
        return file;
    }

    private void testCertificateUsage(Path scratchDir) throws IOException {
        try (AutoCertificateAndKeySource certificateAndKeySource = new AutoCertificateAndKeySource(scratchDir, random)) {
            KeystoreInput keystoreInput = certificateAndKeySource.acquireKeystoreInput();
            SerializableForm serializableForm = certificateAndKeySource.createSerializableForm();
            File keystoreFile = createTempPathname(scratchDir, ".keystore");
            File pkcs12File = createTempPathname(scratchDir, ".p12");
            KeystoreFileCreator keystoreFileCreator = new OpensslKeystoreFileCreator(
                    TestBases.makeKeytoolConfig(), TestBases.makeOpensslConfig());
            keystoreFileCreator.createPKCS12File(keystoreInput, pkcs12File);
            File pemFile = File.createTempFile("certificate", ".pem", scratchDir.toFile());
            assumeOpensslNotSkipped();
            try {
                keystoreFileCreator.createPemFile(pkcs12File, keystoreInput.getPassword(), pemFile);
            } catch (KeystoreFileCreator.NonzeroExitFromCertProgramException e) {
                System.out.println(e.result.content().stderr());
                throw e;
            }
            Files.write(Base64.getDecoder().decode(serializableForm.keystoreBase64), keystoreFile);
            TrafficCollector collector = TrafficCollector.builder(buildHeadlessFactory())
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

    private void assumeOpensslNotSkipped() {
        Assume.assumeFalse("openssl tests are skipped by setting selenium-help.tests.openssl.skip", UnitTests.isSkipOpensslTests());
    }
}