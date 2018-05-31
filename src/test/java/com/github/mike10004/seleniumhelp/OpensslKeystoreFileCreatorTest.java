package com.github.mike10004.seleniumhelp;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Random;

import static org.junit.Assert.*;

public class OpensslKeystoreFileCreatorTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Random random;
    private KeystoreInput keystoreInput;
    private OpensslKeystoreFileCreator keystoreFileCreator;

    @Before
    public void setUp() throws Exception {
        random = new Random(getClass().getName().hashCode());
        String password = "shibboleth";
        try (AutoCertificateAndKeySource certificateAndKeySource = new AutoCertificateAndKeySource(temporaryFolder.getRoot().toPath())) {
            AutoCertificateAndKeySource.MemoryKeyStoreCertificateSource m = certificateAndKeySource.generate(password);
            keystoreInput = KeystoreInput.wrap(m.keystoreBytes, m.keystorePassword).copyFrozen();
        }
        keystoreFileCreator = new OpensslKeystoreFileCreator(UnitTests.makeKeytoolConfig(), UnitTests.makeOpensslConfig());
    }

    @Test
    public void createPKCS12File() throws Exception {
        testCreatePKCS12FileAndPemFile(false);
    }

    @Test
    public void createPemFile() throws Exception {
        testCreatePKCS12FileAndPemFile(true);
    }

    private void testCreatePKCS12FileAndPemFile(boolean createPem) throws Exception {
        File p12File = KeystoreFileCreator.createUniquePathname(temporaryFolder.getRoot(), ".p12", random);
        keystoreFileCreator.createPKCS12File(keystoreInput, p12File);
        assertTrue("p12 file", p12File.isFile() && p12File.length() > 1);
        if (createPem) {
            File pemFile = KeystoreFileCreator.createUniquePathname(temporaryFolder.getRoot(), ".pem", random);
            keystoreFileCreator.createPemFile(p12File, keystoreInput.getPassword(), pemFile);
            assertTrue("pem file", pemFile.isFile() && pemFile.length() > 1);
        }
    }

}