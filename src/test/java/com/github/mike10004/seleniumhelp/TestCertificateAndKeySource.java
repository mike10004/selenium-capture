package com.github.mike10004.seleniumhelp;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import net.lightbody.bmp.mitm.CertificateAndKeySource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public class TestCertificateAndKeySource extends KeyStoreStreamCertificateSource implements FirefoxCompatibleCertificateSource {

    private static final String RESOURCE_DIR = "/selenium-help";

    private TestCertificateAndKeySource() {
        super(CustomCertificate.KEYSTORE_TYPE,
                CustomCertificate.getKeystoreByteSource(),
                CustomCertificate.KEYSTORE_PRIVATE_KEY_ALIAS,
                CustomCertificate.KEYSTORE_PASSWORD);
    }

    public static CertificateAndKeySource create() {
        return new TestCertificateAndKeySource();
    }

    @Override
    public ByteSource getFirefoxCertificateDatabase() throws IOException {
        return Resources.asByteSource(getClass().getResource(RESOURCE_DIR + "/cert8.db"));
    }

    private static class CustomCertificate {
        public static final String KEYSTORE_TYPE = "PKCS12";
        public static final String KEYSTORE_PRIVATE_KEY_ALIAS = "key";
        public static final String KEYSTORE_PASSWORD = "password"; // note: intentionally not secure
        private static final String KEYSTORE_RESOURCE_PATH = RESOURCE_DIR + "/mitm-certificate.keystore";

        public static ByteSource getKeystoreByteSource() {
            return Resources.asByteSource(CustomCertificate.class.getResource(KEYSTORE_RESOURCE_PATH));
        }
    }

    public static char[] getKeystorePassword(CertificateAndKeySource certificateAndKeySource) {
        checkArgument(certificateAndKeySource instanceof TestCertificateAndKeySource);
        return CustomCertificate.KEYSTORE_PASSWORD.toCharArray();
    }

    public static File getKeystoreFile(CertificateAndKeySource certificateAndKeySource) {
        checkArgument(certificateAndKeySource instanceof TestCertificateAndKeySource);
        URL resource = TestCertificateAndKeySource.class.getResource(CustomCertificate.KEYSTORE_RESOURCE_PATH);
        URI resourceUri;
        try {
            resourceUri = resource.toURI();

        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        checkState("file".equals(resourceUri.getScheme()));
        return new File(resourceUri);
    }
}
