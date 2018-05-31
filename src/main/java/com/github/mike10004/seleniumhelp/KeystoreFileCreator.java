package com.github.mike10004.seleniumhelp;

import com.github.mike10004.nativehelper.subprocess.ProcessResult;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public interface KeystoreFileCreator {

    static File createUniquePathname(File parentDir, @Nullable String suffix, Random random) {
        byte[] grist = new byte[24];
        random.nextBytes(grist);
        String filename = BaseEncoding.base16().encode(grist) + Strings.nullToEmpty(suffix);
        File file = new File(parentDir, filename);
        return file;
    }

    void createPKCS12File(KeystoreInput keystore, File p12File) throws IOException;

    void createPemFile(File pkcs12File, String keystorePassword, File pemFile) throws IOException;

    class NonzeroExitFromCertProgramException extends RuntimeException {

        public final ProcessResult<String, String> result;

        public NonzeroExitFromCertProgramException(String executable, ProcessResult<String, String> result) {
            super("exit code " + result.exitCode() + " from " + executable);
            this.result = result;
        }
    }
}
