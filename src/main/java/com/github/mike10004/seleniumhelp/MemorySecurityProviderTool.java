package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.mitm.exception.ImportException;
import net.lightbody.bmp.mitm.exception.KeyStoreAccessException;
import net.lightbody.bmp.mitm.tools.DefaultSecurityProviderTool;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static com.google.common.base.Preconditions.checkNotNull;

public final class MemorySecurityProviderTool extends DefaultSecurityProviderTool {

    @Override
    public KeyStore loadKeyStore(File file, String keyStoreType, String password) {
        throw new UnsupportedOperationException("loading from file not supported by " + getClass());
    }

    public KeyStore loadKeyStore(byte[] bytes, String keyStoreType, char[] password) {
        checkNotNull(bytes, "bytes");
        checkNotNull(password, "password");
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(keyStoreType);
        } catch (KeyStoreException e) {
            throw new KeyStoreAccessException("Unable to get KeyStore instance of type: " + keyStoreType, e);
        }

        try (InputStream keystoreAsStream = new ByteArrayInputStream(bytes)) {
            keyStore.load(keystoreAsStream, password);
        } catch (IOException e) {
            throw new ImportException("Unable to read KeyStore from byte array of length " + bytes.length, e);
        } catch (CertificateException | NoSuchAlgorithmException e) {
            throw new ImportException("Error while reading KeyStore", e);
        }
        return keyStore;
    }
}
