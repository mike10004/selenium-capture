package io.github.mike10004.seleniumcapture.chrome;

import com.github.mike10004.seleniumhelp.DuplicateHeaderBugTest;

public class ChromeDuplicateHeaderBugTest extends DuplicateHeaderBugTest {
    public ChromeDuplicateHeaderBugTest() {
        super(new ChromeTestParameter(true));
    }
}
