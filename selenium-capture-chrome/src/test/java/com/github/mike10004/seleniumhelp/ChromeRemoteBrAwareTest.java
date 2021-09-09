package com.github.mike10004.seleniumhelp;

public class ChromeRemoteBrAwareTest extends BrAwareServerResponseCaptureFilterTest.RemoteTestBase {

    public ChromeRemoteBrAwareTest() {
        super(new ChromeTestParameter(true));
    }

}