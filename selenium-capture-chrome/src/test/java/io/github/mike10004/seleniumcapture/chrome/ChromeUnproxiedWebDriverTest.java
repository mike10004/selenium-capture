package io.github.mike10004.seleniumcapture.chrome;

import io.github.mike10004.seleniumcapture.testbases.UnproxiedWebDriverTest;
import com.github.mike10004.seleniumhelp.WebDriverFactory;

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
