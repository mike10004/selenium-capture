package com.github.mike10004.seleniumhelp;

import com.github.mike10004.seleniumhelp.AutoCertificateAndKeySourceTestBase;
import com.github.mike10004.seleniumhelp.ChromeTestParameter;
import com.github.mike10004.seleniumhelp.ChromeWebDriverFactory;
import com.github.mike10004.seleniumhelp.UnitTests;
import com.github.mike10004.seleniumhelp.WebDriverFactory;
import com.github.mike10004.seleniumhelp.WebDriverTestParameter;
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
