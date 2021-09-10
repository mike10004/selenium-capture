package io.github.mike10004.seleniumcapture;

import com.github.mike10004.seleniumhelp.FirefoxTestParameter;
import com.github.mike10004.seleniumhelp.FirefoxUnitTests;
import com.github.mike10004.seleniumhelp.WebDriverFactory;

public class FirefoxBrowserUpHarsTest extends BrowserUpHarsTest {

    public FirefoxBrowserUpHarsTest() {
        super(new FirefoxTestParameter());
    }

    @Override
    protected WebDriverFactory buildHeadlessFactory() {
        return FirefoxUnitTests.headlessWebDriverFactory();
    }
}
