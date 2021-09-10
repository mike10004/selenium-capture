package io.github.mike10004.seleniumcapture.firefox;

import io.github.mike10004.seleniumcapture.testbases.BrAwareServerResponseCaptureFilterTest;

public class FirefoxRemoteBrAwareTest extends BrAwareServerResponseCaptureFilterTest.RemoteTestBase {

    public FirefoxRemoteBrAwareTest() {
        super(new FirefoxTestParameter(true));
    }

}
