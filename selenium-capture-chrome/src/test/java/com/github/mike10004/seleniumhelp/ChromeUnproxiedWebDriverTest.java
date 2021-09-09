package com.github.mike10004.seleniumhelp;

public class ChromeUnproxiedWebDriverTest extends UnproxiedWebDriverTest {

    @Override
    protected WebDriverFactory buildWebDriverFactory() {
        return ChromeWebDriverFactory.builder()
                .configure(ChromeUnitTests.createChromeOptions())
                .configure(o -> o.setHeadless(true))
                .build();
    }

    @Override
    protected void setupWebdriver() {
        ChromeUnitTests.setupRecommendedChromeDriver();
    }
}
