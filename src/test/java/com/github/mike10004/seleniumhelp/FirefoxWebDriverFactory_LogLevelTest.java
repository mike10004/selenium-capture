package com.github.mike10004.seleniumhelp;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.logging.Level;

import static org.junit.Assert.assertTrue;

public class FirefoxWebDriverFactory_LogLevelTest {

    private static final boolean verbose = true;

    @BeforeClass
    public static void setupGeckodriver() {
        UnitTests.setupRecommendedGeckoDriver();
    }

    @Test
    public void testSetLogLevelReducesOutput() throws Exception {
        FirefoxWebDriverFactory wdf_verboseLogLevelSet = FirefoxWebDriverFactory.builder()
                .webdriverLogLevel(Level.FINER)
                .configure(o -> o.setHeadless(true))
                .build();
        String verboseStderr = captureStderr(wdf_verboseLogLevelSet);
        FirefoxWebDriverFactory wdf_terseLogLevelSet = FirefoxWebDriverFactory.builder()
                .configure(o -> o.setHeadless(true))
                .webdriverLogLevel(Level.WARNING)
                .build();
        String terseStderr = captureStderr(wdf_terseLogLevelSet);
        assertTrue("less output when level is less verbose", verboseStderr.length() > terseStderr.length());
    }

    private String captureStderr(FirefoxWebDriverFactory wdf) throws IOException {
        System.out.format("capturing stderr for session with %s%n", wdf);
        ProcessStreamCaptor captor = new ProcessStreamCaptor(1024);
        try (ProcessStreamCaptor.CaptureScope ignore = captor.start()) {
            try (WebdrivingSession session = wdf.startWebdriving(WebdrivingConfig.inactive())) {
                WebDriver driver = session.getWebDriver();
                driver.get("https://microsoft.com/");
            }
        }
        String stdout = captor.dumpStdout();
        System.out.format("%d chars in stdout%n", stdout.length());
        String stderr = captor.dumpStderr();
        System.out.format("%d chars in stderr%n", stderr.length());
        if (verbose) {
            System.out.println("=============================================================");
            System.out.println("=============================================================");
            System.out.println(stderr);
            System.out.println("=============================================================");
            System.out.println("=============================================================");
        }
        return stderr;
    }

    private static class ProcessStreamCaptor {

        private final Charset charset;
        private final ByteArrayOutputStream stdoutBucket, stderrBucket;

        public ProcessStreamCaptor(int initialCapacity) {
            stdoutBucket = new ByteArrayOutputStream(initialCapacity);
            stderrBucket = new ByteArrayOutputStream(initialCapacity);
            charset = Charset.defaultCharset();
        }

        public String dumpStdout() {
            return new String(stdoutBucket.toByteArray(), charset);
        }

        public String dumpStderr() {
            return new String(stderrBucket.toByteArray(), charset);
        }

        public CaptureScope start() {
            return new CaptureScope();
        }

        private class CaptureScope implements AutoCloseable {

            private final PrintStream stdout, stderr;

            public CaptureScope() {
                this.stdout = System.out;
                this.stderr = System.err;
                try {
                    System.setOut(new PrintStream(stdoutBucket, true, charset.name()));
                } catch (UnsupportedEncodingException e) {
                    resetProcessStreams();
                    throw new AssertionError("default encoding shouldn't cause this", e);
                }
                try {
                    System.setErr(new PrintStream(stderrBucket, true, charset.name()));
                } catch (UnsupportedEncodingException e) {
                    resetProcessStreams();
                    throw new AssertionError("default encoding shouldn't cause this", e);
                }
            }

            private void resetProcessStreams() {
                System.setOut(stdout);
                System.setErr(stderr);
            }

            @Override
            public final void close() {
                stdout.flush();
                stderr.flush();
                resetProcessStreams();
            }
        }
    }

}
