package io.github.mike10004.seleniumcapture.firefox;

import com.github.mike10004.seleniumhelp.WebDriverFactory;
import io.github.mike10004.seleniumcapture.testbases.TrafficCollectorTest;

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
