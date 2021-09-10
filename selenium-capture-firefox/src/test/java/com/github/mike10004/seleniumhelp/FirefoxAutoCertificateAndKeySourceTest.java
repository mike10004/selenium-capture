package com.github.mike10004.seleniumhelp;

import io.github.mike10004.seleniumcapture.testbases.AutoCertificateAndKeySourceTestBase;

public class FirefoxAutoCertificateAndKeySourceTest extends AutoCertificateAndKeySourceTestBase {

    public FirefoxAutoCertificateAndKeySourceTest() {

    }

    @Override
    protected void setupWebDriver() {
        FirefoxUnitTests.setupRecommendedGeckoDriver();
    }

    @Override
    protected WebDriverFactory buildHeadlessFactory() {
        return FirefoxUnitTests.headlessWebDriverFactory(true);
    }
}
