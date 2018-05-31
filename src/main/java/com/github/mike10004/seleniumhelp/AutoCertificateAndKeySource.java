package com.github.mike10004.seleniumhelp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import net.lightbody.bmp.mitm.CertificateAndKey;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.RootCertificateGenerator;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Random;

import static com.google.common.base.Preconditions.checkState;

/**
 * Auto-generating certificate and key source.
 */
public class AutoCertificateAndKeySource implements CertificateAndKeySource, java.io.Closeable {

    private static final Logger log = LoggerFactory.getLogger(AutoCertificateAndKeySource.class);

    private volatile MemoryKeyStoreCertificateSource onDemandSource;
    private volatile boolean closed;
    private transient final Object generationLock = new Object();

    private final Path scratchDir;
    private final Random random;

    @SuppressWarnings("unused")
    public AutoCertificateAndKeySource(Path scratchDir) {
        this(scratchDir, new Random());
    }

    @VisibleForTesting
    AutoCertificateAndKeySource(Path scratchDir, Random random) {
        this.scratchDir = scratchDir;
        this.random = random;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public void close() throws IOException {
        synchronized (generationLock) {
            if (onDemandSource != null) {
                Arrays.fill(onDemandSource.keystoreBytes, (byte) 0);
                closed = true;
                onDemandSource = null;
            }
        }
    }

    protected void keystorePasswordGenerated(String password) {
        // no op
    }

    private void generateIfNecessary() {
        synchronized (generationLock) {
            checkState(!closed, "this source is closed");
            if (onDemandSource == null) {
                try {
                    byte[] bytes = new byte[32];
                    random.nextBytes(bytes);
                    String password = Base64.getEncoder().encodeToString(bytes);
                    keystorePasswordGenerated(password);
                    onDemandSource = generate(password);
                } catch (IOException e) {
                    throw new CertificateGenerationException(e);
                }
            }
        }

    }

    @Override
    public CertificateAndKey load() {
        generateIfNecessary();
        return onDemandSource.load();
    }

    static class CertificateGenerationException extends RuntimeException {
        public CertificateGenerationException(String message) {
            super(message);
        }

        @SuppressWarnings("unused")
        public CertificateGenerationException(String message, Throwable cause) {
            super(message, cause);
        }

        public CertificateGenerationException(Throwable cause) {
            super(cause);
        }
    }

    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String KEYSTORE_PRIVATE_KEY_ALIAS = "key";

    protected static class MemoryKeyStoreCertificateSource extends KeyStoreStreamCertificateSource {

        public final byte[] keystoreBytes;
        public final String keystorePassword;

        public MemoryKeyStoreCertificateSource(String keyStoreType, byte[] keystoreBytes, String privateKeyAlias, String keyStorePassword) {
            super(keyStoreType, ByteSource.wrap(keystoreBytes), privateKeyAlias, keyStorePassword);
            this.keystoreBytes = Objects.requireNonNull(keystoreBytes);
            this.keystorePassword = Objects.requireNonNull(keyStorePassword);
        }

    }

    public static class SerializableForm {
        public String keystoreBase64;
        public String password;

        public SerializableForm(String keystoreBase64, String password) {
            this.keystoreBase64 = keystoreBase64;
            this.password = password;
        }
    }

    @SuppressWarnings("RedundantThrows")
    public SerializableForm createSerializableForm() throws IOException {
        generateIfNecessary();
        return new SerializableForm(Base64.getEncoder().encodeToString(onDemandSource.keystoreBytes), onDemandSource.keystorePassword);
    }

    protected void keystoreBytesGenerated(ByteSource byteSource) {
        // no op; subclasses may override
    }

    protected MemoryKeyStoreCertificateSource generate(String keystorePassword) throws IOException {
        File keystoreFile = File.createTempFile("dynamically-generated-certificate", ".keystore", scratchDir.toFile());
        try {
            // create a dynamic CA root certificate generator using default settings (2048-bit RSA keys)
            RootCertificateGenerator rootCertificateGenerator = RootCertificateGenerator.builder().build();
            rootCertificateGenerator.saveRootCertificateAndKey(KEYSTORE_TYPE, keystoreFile, KEYSTORE_PRIVATE_KEY_ALIAS, keystorePassword);
            log.debug("saved keystore to {} ({} bytes)%n", keystoreFile, keystoreFile.length());
            byte[] keystoreBytes = Files.toByteArray(keystoreFile);
            keystoreBytesGenerated(ByteSource.wrap(keystoreBytes));
            return new MemoryKeyStoreCertificateSource(KEYSTORE_TYPE, keystoreBytes, KEYSTORE_PRIVATE_KEY_ALIAS, keystorePassword);
        } finally {
            FileUtils.forceDelete(keystoreFile);
        }
    }

    public KeystoreInput acquireKeystoreInput() {
        return new KeystoreInput() {
            @Override
            public ByteSource getBytes() {
                generateIfNecessary();
                return ByteSource.wrap(onDemandSource.keystoreBytes);
            }

            @Override
            public String getPassword() {
                generateIfNecessary();
                return onDemandSource.keystorePassword;
            }
        };
    }
}
