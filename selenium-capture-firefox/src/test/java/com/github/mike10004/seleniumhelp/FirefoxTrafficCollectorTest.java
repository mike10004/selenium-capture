package com.github.mike10004.seleniumhelp;

public class FirefoxTrafficCollectorTest extends TrafficCollectorTest {
    public FirefoxTrafficCollectorTest() {

    }

    @Override
    protected WebDriverFactory createHeadlessFactory() {
        return FirefoxUnitTests.headlessWebDriverFactoryBuilder()
                .disableTrackingProtection()
                .disableRemoteSettings()
                .build();
    }

}
