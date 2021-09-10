package com.github.mike10004.seleniumhelp;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class AutoCertificateAndKeySourceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void invokeLoadTwice() throws Exception {
        Path scratchDir = temporaryFolder.getRoot().toPath();
        Random random = new Random(AutoCertificateAndKeySourceTest.class.getName().hashCode());
        try (CountingAutoCertificateAndKeySource certificateAndKeySource = new CountingAutoCertificateAndKeySource(scratchDir, random)) {
            certificateAndKeySource.load();
            certificateAndKeySource.load();
            assertEquals("num generate invocations", 1, certificateAndKeySource.generateInvocations.get());
        }
    }
    private static class CountingAutoCertificateAndKeySource extends AutoCertificateAndKeySource {

        private final AtomicInteger generateInvocations = new AtomicInteger();

        public CountingAutoCertificateAndKeySource(Path scratchDir, Random random) {
            super(scratchDir, random);
        }

        @Override
        protected MemoryKeyStoreCertificateSource generate(String keystorePassword) throws IOException {
            generateInvocations.incrementAndGet();
            return super.generate(keystorePassword);
        }
    }

}