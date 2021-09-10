package io.github.mike10004.seleniumcapture.testbases;

import com.github.mike10004.seleniumhelp.ExecutableConfig;
import com.google.common.io.Resources;
import io.github.bonigarcia.wdm.config.DriverManagerType;
import io.github.mike10004.seleniumcapture.testing.UnitTests;

import java.io.File;
import java.io.IOException;

public class TestBases {

    public static void setupWebDriver(DriverManagerType dmt) {
        DriverManagerSetupCache.doSetup(dmt);
    }

    static byte[] loadBrotliUncompressedSample() throws IOException {
        return Resources.toByteArray(BrAwareServerResponseCaptureFilterTest.class.getResource("/brotli/a100.txt"));
    }

    static byte[] loadBrotliCompressedSample() throws IOException {
        return Resources.toByteArray(BrAwareServerResponseCaptureFilterTest.class.getResource("/brotli/a100.txt.br"));
    }

    public static ExecutableConfig makeOpensslConfig() {
        String path = UnitTests.Settings.get(UnitTests.SETTING_OPENSSL_EXECUTABLE_PATH);
        if (path != null) {
            File file = new File(path);
            System.out.format("using openssl executable at %s%n", file);
            return ExecutableConfig.byPathOnly(file);
        }
        return ExecutableConfig.byNameOnly("openssl");
    }

    public static ExecutableConfig makeKeytoolConfig() {
        return ExecutableConfig.byNameOnly("keytool");
    }
}
