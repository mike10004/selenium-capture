package io.github.mike10004.seleniumcapture.testbases;

import io.github.mike10004.seleniumcapture.KeyStoreStreamCertificateSource;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.browserup.bup.mitm.CertificateAndKeySource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class TestCertificateAndKeySource extends KeyStoreStreamCertificateSource {

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

    public static final String CERTIFICATE_DB_FILENAME = "cert8.db";

    private static class GunzippingByteSource extends ByteSource {

        private final ByteSource gzippedByteSource;

        private GunzippingByteSource(ByteSource gzippedByteSource) {
            this.gzippedByteSource = checkNotNull(gzippedByteSource);
        }

        @Override
        public InputStream openStream() throws IOException {
            return new GZIPInputStream(gzippedByteSource.openStream());
        }
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

    public static ByteSource getCertificatePemByteSource() {
        URL resource = TestCertificateAndKeySource.class.getResource(RESOURCE_DIR + "/mitm-certificate.pem.gz");
        return new GunzippingByteSource(Resources.asByteSource(resource));
    }
}
