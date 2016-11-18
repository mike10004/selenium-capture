package com.github.mike10004.seleniumhelp;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.KeyStoreFileCertificateSource;

import java.io.IOException;

public class TestCertificateAndKeySource extends KeyStoreStreamCertificateSource implements FirefoxCompatibleCertificateSource {

    private static final String RESOURCE_DIR = "/selenium-help";

    public TestCertificateAndKeySource() {
        super(CustomCertificate.KEYSTORE_TYPE,
                CustomCertificate.getKeystoreByteSource(),
                CustomCertificate.KEYSTORE_PRIVATE_KEY_ALIAS,
                CustomCertificate.KEYSTORE_PASSWORD);
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


}
