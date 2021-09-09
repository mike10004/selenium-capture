package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbtesting.XvfbRule;
import io.github.bonigarcia.wdm.config.DriverManagerType;

public class ChromeTestParameter implements WebDriverTestParameter {

    private final boolean acceptInsecureCerts;

    public ChromeTestParameter() {
        this(false);
    }

    public ChromeTestParameter(boolean acceptInsecureCerts) {
        this.acceptInsecureCerts = acceptInsecureCerts;
    }

    @Override
    public WebDriverFactory createWebDriverFactory(XvfbRule xvfb) {
        return ChromeWebDriverFactory.builder()
                .configure(ChromeUnitTests.createChromeOptions())
                .configure(o -> o.setAcceptInsecureCerts(acceptInsecureCerts))
                .environment(xvfb.getController().newEnvironment())
                .build();
    }

    @Override
    public boolean isBrotliSupported(String url) {
        return true;
    }

    @Override
    public DriverManagerType getDriverManagerType() {
        return DriverManagerType.CHROME;
    }
}
