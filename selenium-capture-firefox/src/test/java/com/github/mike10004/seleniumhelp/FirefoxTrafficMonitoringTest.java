package com.github.mike10004.seleniumhelp;

import io.github.bonigarcia.wdm.config.DriverManagerType;
import io.github.mike10004.seleniumcapture.testbases.TrafficMonitoringTest;

public class FirefoxTrafficMonitoringTest extends TrafficMonitoringTest {

    public FirefoxTrafficMonitoringTest() {
        super(DriverManagerType.FIREFOX);
    }

    @Override
    protected WebDriverFactory createWebDriverFactory(boolean acceptInsecureCerts) {
        return FirefoxUnitTests.headlessWebDriverFactoryBuilder(acceptInsecureCerts)
                .disableRemoteSettings()
                .build();
    }
}
