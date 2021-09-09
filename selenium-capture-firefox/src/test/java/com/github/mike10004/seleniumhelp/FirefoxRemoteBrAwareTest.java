package com.github.mike10004.seleniumhelp;

public class FirefoxRemoteBrAwareTest extends BrAwareServerResponseCaptureFilterTest.RemoteTestBase {

    public FirefoxRemoteBrAwareTest() {
        super(new FirefoxTestParameter(true));
    }

}
