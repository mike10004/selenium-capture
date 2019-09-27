package com.github.mike10004.seleniumhelp;

import io.github.mike10004.subprocess.SubprocessException;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.Subprocess;
import com.google.common.io.ByteSource;

import javax.annotation.Nullable;
import java.nio.charset.Charset;

class Subprocesses {

    public static ProcessResult<String, String> executeAndWait(Subprocess subprocess, Charset charset, @Nullable ByteSource stdin) throws InterruptedException, SubprocessException {
        ProcessResult<String, String> result;
        try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
            result = subprocess.launcher(processTracker)
                    .outputStrings(charset, stdin == null ? null : stdin::openStream)
                    .launch().await();
        }
        return result;
    }
}
