package io.github.mike10004.seleniumcapture.firefox;

import io.github.mike10004.seleniumcapture.WebDriverFactory;
import io.github.mike10004.seleniumcapture.testbases.UnproxiedWebDriverTest;

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
