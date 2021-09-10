package io.github.mike10004.seleniumcapture.firefox;

import com.github.mike10004.seleniumhelp.WebDriverFactory;
import io.github.mike10004.seleniumcapture.testbases.BrowserUpHarsTestBase;

public class FirefoxBrowserUpHarsTest extends BrowserUpHarsTestBase {

    public FirefoxBrowserUpHarsTest() {
        super(new FirefoxTestParameter());
    }

    @Override
    protected WebDriverFactory buildHeadlessFactory() {
        return FirefoxUnitTests.headlessWebDriverFactory();
    }
}
