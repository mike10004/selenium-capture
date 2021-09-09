package com.github.mike10004.seleniumhelp;

public class FirefoxUnproxiedWebDriverTest extends UnproxiedWebDriverTest {

    @Override
    protected WebDriverFactory buildWebDriverFactory() {
        return FirefoxWebDriverFactory.builder()
                .binary(FirefoxUnitTests.createFirefoxBinarySupplier())
                .environment(xvfbRule.getController().newEnvironment())
                .build();
    }

    @Override
    protected void setupWebdriver() {
        FirefoxUnitTests.setupRecommendedGeckoDriver();
    }
}
