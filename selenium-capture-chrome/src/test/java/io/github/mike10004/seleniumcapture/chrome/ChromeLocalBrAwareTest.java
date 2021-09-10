package io.github.mike10004.seleniumcapture.chrome;

import com.github.mike10004.seleniumhelp.BrAwareServerResponseCaptureFilterTest;

public class ChromeLocalBrAwareTest extends BrAwareServerResponseCaptureFilterTest.LocalTestBase {

    public ChromeLocalBrAwareTest() {
        super(new ChromeTestParameter(false));
    }

}
