package com.github.mike10004.seleniumhelp;

import io.github.mike10004.subprocess.Subprocess;

class ProcessWaitingInterruptedException extends RuntimeException {
    public ProcessWaitingInterruptedException(Subprocess subprocess, InterruptedException e) {
        super("waiting for " + subprocess + " to finish was interrupted", e);
    }
}
