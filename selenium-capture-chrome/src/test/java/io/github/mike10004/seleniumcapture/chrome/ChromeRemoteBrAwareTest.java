package io.github.mike10004.seleniumcapture.chrome;

import io.github.mike10004.seleniumcapture.testbases.BrAwareServerResponseCaptureFilterTest;

public class ChromeRemoteBrAwareTest extends BrAwareServerResponseCaptureFilterTest.RemoteTestBase {

    public ChromeRemoteBrAwareTest() {
        super(new ChromeTestParameter(true));
    }

}