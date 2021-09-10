package com.github.mike10004.seleniumhelp;

import io.github.mike10004.subprocess.SubprocessException;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.Subprocess;
import com.google.common.io.ByteSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.sql.SQLException;

/**
 * For internal use only.
 */
public class Subprocesses {

    public static ProcessResult<String, String> executeAndWait(Subprocess subprocess, Charset charset, @Nullable ByteSource stdin) throws InterruptedException, SubprocessException {
        ProcessResult<String, String> result;
        try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
            result = subprocess.launcher(processTracker)
                    .outputStrings(charset, stdin == null ? null : stdin::openStream)
                    .launch().await();
        }
        return result;
    }

    public static ProcessResult<String, String> executeOrPropagateInterruption(Subprocess subprocess,
                                                                               Charset ioEncoding,
                                                                               @Nullable ByteSource stdinSource) throws ProcessWaitingInterruptedException {
        try {
            return executeAndWait(subprocess, ioEncoding, stdinSource);
        } catch (InterruptedException e) {
            throw new ProcessWaitingInterruptedException(subprocess, e);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(Subprocesses.class);

    public static void checkResult(ProcessResult<String, String> result) throws SQLException {
        if (result.exitCode() != 0) {
            log.error("sqlite3 exited with code {}; stderr: {}", result.exitCode(), result.content().stderr());
            throw new SQLException("sqlite3 exited with code " + result.exitCode() + "; " + StringUtils.abbreviate(result.content().stderr(), 256));
        }
    }

}
