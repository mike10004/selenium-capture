package io.github.mike10004.seleniumcapture.chrome;

import io.github.mike10004.seleniumcapture.testbases.AutoCertificateAndKeySourceTestBase;
import io.github.mike10004.seleniumcapture.testing.UnitTests;
import io.github.mike10004.seleniumcapture.WebDriverFactory;
import io.github.mike10004.seleniumcapture.testbases.WebDriverTestParameter;
import com.github.mike10004.xvfbtesting.XvfbRule;
import org.junit.ClassRule;

public class ChromeAutoCertificateAndKeySourceTest extends AutoCertificateAndKeySourceTestBase {

    @ClassRule
    public static final XvfbRule xvfb = UnitTests.xvfbRuleBuilder().build();

    private final WebDriverTestParameter webDriverTestParameter;

    public ChromeAutoCertificateAndKeySourceTest() {
        webDriverTestParameter = new ChromeTestParameter();
    }

    @Override
    protected void setupWebDriver() {
        webDriverTestParameter.doDriverManagerSetup();
    }

    @Override
    protected WebDriverFactory buildHeadlessFactory() {
        return ChromeWebDriverFactory.builder()
                .acceptInsecureCerts(false)
                .environment(() -> xvfb.getController().newEnvironment())
                .build();
    }
}
