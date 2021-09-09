package com.github.mike10004.seleniumhelp;

public class FirefoxAutoCertificateAndKeySourceTest extends AutoCertificateAndKeySourceTest {

    public FirefoxAutoCertificateAndKeySourceTest() {

    }

    @Override
    protected void setupWebDriver() {
        FirefoxUnitTests.setupRecommendedGeckoDriver();
    }

    @Override
    protected WebDriverFactory buildHeadlessFactory() {
        return FirefoxUnitTests.headlessWebDriverFactory();
    }
}
