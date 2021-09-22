package io.github.mike10004.seleniumcapture;

public class SeleniumCaptureException extends RuntimeException {
    public SeleniumCaptureException() {
    }

    public SeleniumCaptureException(String message) {
        super(message);
    }

    public SeleniumCaptureException(String message, Throwable cause) {
        super(message, cause);
    }

    public SeleniumCaptureException(Throwable cause) {
        super(cause);
    }
}
