package io.github.mike10004.seleniumcapture.chrome;

import com.github.mike10004.seleniumhelp.BrAwareServerResponseCaptureFilterTest;

public class ChromeRemoteBrAwareTest extends BrAwareServerResponseCaptureFilterTest.RemoteTestBase {

    public ChromeRemoteBrAwareTest() {
        super(new ChromeTestParameter(true));
    }

}