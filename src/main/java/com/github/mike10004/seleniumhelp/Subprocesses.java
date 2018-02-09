package com.github.mike10004.seleniumhelp;

import com.github.mike10004.nativehelper.subprocess.ProcessException;
import com.github.mike10004.nativehelper.subprocess.ProcessResult;
import com.github.mike10004.nativehelper.subprocess.ScopedProcessTracker;
import com.github.mike10004.nativehelper.subprocess.Subprocess;
import com.google.common.io.ByteSource;

import javax.annotation.Nullable;
import java.nio.charset.Charset;

class Subprocesses {

    public static ProcessResult<String, String> executeAndWait(Subprocess subprocess, Charset charset, @Nullable ByteSource stdin) throws InterruptedException, ProcessException {
        ProcessResult<String, String> result;
        try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
            result = subprocess.launcher(processTracker)
                    .outputStrings(charset, stdin)
                    .launch().await();
        }
        return result;
    }
}
