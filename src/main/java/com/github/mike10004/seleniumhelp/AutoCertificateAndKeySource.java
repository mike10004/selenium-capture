package com.github.mike10004.seleniumhelp;

import com.github.mike10004.nativehelper.subprocess.ProcessResult;
import com.github.mike10004.nativehelper.subprocess.ScopedProcessTracker;
import com.github.mike10004.nativehelper.subprocess.Subprocess;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import net.lightbody.bmp.mitm.CertificateAndKey;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.RootCertificateGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkState;

/**
 * Auto-generating certificate and key source. Basic usage of this class
 * doesn't have external dependencies, but generating PKCS12 and PEM files
 * requires {@code keytool} and {@code openssl} be installed.
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

    protected void passwordGenerated(String password) {
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
                    passwordGenerated(password);
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

    private static class MemoryKeyStoreCertificateSource extends KeyStoreStreamCertificateSource {

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

    public SerializableForm createSerializableForm() throws IOException {
        generateIfNecessary();
        return new SerializableForm(Base64.getEncoder().encodeToString(onDemandSource.keystoreBytes), onDemandSource.keystorePassword);
    }

    protected void keystoreBytesGenerated(ByteSource byteSource) {
        // no op
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

    protected String getKeytoolExecutableName() {
        return "keytool";
    }

    /**
     * Creates a PKCS12-format file.
     *
     * <p>
     * See https://stackoverflow.com/questions/652916/converting-a-java-keystore-into-pem-format
     * for instructions on how to convert the .keystore file into a .pem file that can be installed
     * into browsers like Firefox.
     *
     * <p>In short, if {@code $KEYSTORE_FILE} is the file generated by this program, execute:
     * <pre>
     *     $ keytool -importkeystore -srckeystore $KEYSTORE_FILE -destkeystore temp.p12 -srcstoretype jks  -deststoretype pkcs12
     *     $ openssl pkcs12 -in temp.p12 -out exported-keystore.pem
     * </pre>
     * <p>The contents of `exported-keystore.pem` will be in PEM format.
     */
    public void createPKCS12File(File p12File) throws IOException, InterruptedException {
        generateIfNecessary();
        File keystoreFile = File.createTempFile("AutoCertificateAndKeySource", ".keystore", scratchDir.toFile());
        try {
            Files.write(onDemandSource.keystoreBytes, keystoreFile);
            String keystorePassword = onDemandSource.keystorePassword;
            {
                Subprocess program = Subprocess.running(getKeytoolExecutableName())
                        .arg("-importkeystore")
                        .args("-srckeystore", keystoreFile.getAbsolutePath())
                        .args("-srcstoretype", "jks")
                        .args("-srcstorepass", keystorePassword)
                        .args("-destkeystore", p12File.getAbsolutePath())
                        .args("-deststoretype", "pkcs12")
                        .args("-deststorepass", keystorePassword)
                        .build();
                executeSubprocessAndCheckResult(program, keytoolResult -> {
                    throw new CertificateGenerationException("nonzero exit from keytool or invalid pkcs12 file length: " + keytoolResult.exitCode());
                });
                if (p12File.length() <= 0) {
                    throw new CertificateGenerationException("pkcs12 file has invalid length: " + p12File.length());
                }
                log.debug("pkcs12: {} ({} bytes)%n", p12File, p12File.length());
            }
        } finally {
            FileUtils.forceDelete(keystoreFile);
        }
    }

    protected String getOpensslExecutableName() {
        return "openssl";
    }

    public void createPemFile(File pkcs12File, File pemFile) throws IOException, InterruptedException {
        generateIfNecessary();
        String keystorePassword = onDemandSource.keystorePassword;
        {
            Subprocess subprocess = Subprocess.running(getOpensslExecutableName())
                    .arg("pkcs12")
                    .args("-in", pkcs12File.getAbsolutePath())
                    .args("-passin", "pass:" + keystorePassword)
                    .args("-out", pemFile.getAbsolutePath())
                    .args("-passout", "pass:")
                    .build();
            executeSubprocessAndCheckResult(subprocess, opensslResult -> {
                return new CertificateGenerationException("nonzero exit from openssl: " + opensslResult.exitCode());
            });
        }
        log.debug("pem: {} ({} bytes)", pemFile, pemFile.length());
    }

    private void executeSubprocessAndCheckResult(Subprocess subprocess, Function<ProcessResult<String, String>, ? extends RuntimeException> nonzeroExitReaction) throws IOException, InterruptedException {
        ProcessResult<String, String> result = Subprocesses.executeAndWait(subprocess, Charset.defaultCharset(), null);
        if (result.exitCode() != 0) {
            log.error("stderr {}", StringUtils.abbreviateMiddle(result.content().stderr(), "[...]", 256));
            log.error("stdout {}", StringUtils.abbreviateMiddle(result.content().stdout(), "[...]", 256));
            RuntimeException ex = nonzeroExitReaction.apply(result);
            if (ex != null) {
                throw ex;
            }
        }
    }
}
