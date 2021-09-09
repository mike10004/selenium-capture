package com.github.mike10004.seleniumhelp;

public class ChromeLocalBrAwareTest extends BrAwareServerResponseCaptureFilterTest.LocalTestBase {

    public ChromeLocalBrAwareTest() {
        super(new ChromeTestParameter(false));
    }

}
