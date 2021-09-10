package io.github.mike10004.seleniumcapture.chrome;

import io.github.mike10004.seleniumcapture.testbases.BrAwareServerResponseCaptureFilterTest;

public class ChromeLocalBrAwareTest extends BrAwareServerResponseCaptureFilterTest.LocalTestBase {

    public ChromeLocalBrAwareTest() {
        super(new ChromeTestParameter(false));
    }

}
