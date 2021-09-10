package com.github.mike10004.seleniumhelp;

import io.github.mike10004.seleniumcapture.testbases.BrAwareServerResponseCaptureFilterTest;

public class FirefoxLocalBrAwareTest  extends BrAwareServerResponseCaptureFilterTest.LocalTestBase {
    public FirefoxLocalBrAwareTest() {
        super(new FirefoxTestParameter(false));
    }

}
