package com.github.mike10004.seleniumhelp;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.io.ByteSource;
import net.lightbody.bmp.mitm.CertificateAndKey;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.KeyStoreCertificateSource;
import net.lightbody.bmp.mitm.exception.CertificateSourceException;
import net.lightbody.bmp.mitm.tools.SecurityProviderTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyStore;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Loads a KeyStore from a file or classpath resource. If configured with a File object, attempts to load the KeyStore
 * from the specified file. Otherwise, attempts to load the KeyStore from a classpath resource.
 */
public class KeyStoreStreamCertificateSource implements CertificateAndKeySource {
    private static final Logger log = LoggerFactory.getLogger(net.lightbody.bmp.mitm.KeyStoreFileCertificateSource.class);

    private final ByteSource keyStoreByteSource;

    private final String keyStoreType;

    private final String keyStorePassword;
    private final String privateKeyAlias;

    private final MemorySecurityProviderTool securityProviderTool;

    private final Supplier<CertificateAndKey> certificateAndKey = Suppliers.memoize(() -> loadKeyStore());

    /**
     * Creates a {@link CertificateAndKeySource} that loads an existing {@link KeyStore} from a classpath resource.
     * @param keyStoreType              the KeyStore type, such as PKCS12 or JKS
     * @param keyStoreByteSource        the byte source providing the keystore bytes
     * @param privateKeyAlias           the alias of the private key in the KeyStore
     * @param keyStorePassword          te KeyStore password
     */
    public KeyStoreStreamCertificateSource(String keyStoreType, ByteSource keyStoreByteSource,
                                           String privateKeyAlias, String keyStorePassword) {
        this(keyStoreType, keyStoreByteSource, privateKeyAlias, keyStorePassword, new MemorySecurityProviderTool());
    }

    public KeyStoreStreamCertificateSource(String keyStoreType, ByteSource keyStoreByteSource,
                                           String privateKeyAlias, String keyStorePassword,
                                           MemorySecurityProviderTool securityProviderTool) {
        this.keyStoreType = checkNotNull(keyStoreType);
        this.keyStoreByteSource = checkNotNull(keyStoreByteSource);
        this.privateKeyAlias = checkNotNull(privateKeyAlias);
        this.keyStorePassword = keyStorePassword;
        this.securityProviderTool = checkNotNull(securityProviderTool);
    }

    @Override
    public CertificateAndKey load() {
        return certificateAndKey.get();

    }

    /**
     * Loads the {@link CertificateAndKey} from the KeyStore using the {@link SecurityProviderTool}.
     */
    private CertificateAndKey loadKeyStore() {
        final KeyStore keyStore;
        byte[] keyStoreBytes;
        try {
            keyStoreBytes = keyStoreByteSource.read();
        } catch (IOException e) {
            throw new CertificateSourceException("Unable to open KeyStore byte source: " + keyStoreByteSource, e);
        }
        char[] keyStorePasswordChars = keyStorePassword.toCharArray();
        keyStore = securityProviderTool.loadKeyStore(keyStoreBytes, keyStoreType, keyStorePasswordChars);
        Arrays.fill(keyStorePasswordChars, '\0');
        Arrays.fill(keyStoreBytes, (byte) 0);
        KeyStoreCertificateSource keyStoreCertificateSource = new KeyStoreCertificateSource(keyStore, privateKeyAlias, keyStorePassword);
        return keyStoreCertificateSource.load();
    }
}
