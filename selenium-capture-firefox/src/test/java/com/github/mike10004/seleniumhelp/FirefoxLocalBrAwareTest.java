package com.github.mike10004.seleniumhelp;

public class FirefoxLocalBrAwareTest  extends BrAwareServerResponseCaptureFilterTest.LocalTestBase {
    public FirefoxLocalBrAwareTest() {
        super(new FirefoxTestParameter(false));
    }

}
