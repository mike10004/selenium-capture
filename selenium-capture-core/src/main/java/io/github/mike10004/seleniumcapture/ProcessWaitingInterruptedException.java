package io.github.mike10004.seleniumcapture;

import io.github.mike10004.subprocess.Subprocess;

public class ProcessWaitingInterruptedException extends RuntimeException {
    public ProcessWaitingInterruptedException(Subprocess subprocess, InterruptedException e) {
        super("waiting for " + subprocess + " to finish was interrupted", e);
    }
}
