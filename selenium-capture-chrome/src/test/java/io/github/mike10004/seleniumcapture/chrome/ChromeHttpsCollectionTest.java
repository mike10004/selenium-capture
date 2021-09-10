package io.github.mike10004.seleniumcapture.chrome;

import com.github.mike10004.seleniumhelp.CollectionTestBase;
import com.github.mike10004.seleniumhelp.WebDriverFactory;
import org.junit.Assume;
import org.junit.Test;

public class ChromeHttpsCollectionTest extends CollectionTestBase {

    public ChromeHttpsCollectionTest() {
        super(new ChromeTestParameter(true), "https");
    }

    @Test
    public void https() throws Exception {
        testhttps(false);
    }

    @Test
    public void https_headless() throws Exception {
        testhttps(true);
    }

    private void testhttps(boolean headless) throws Exception {
        Assume.assumeFalse("headless tests disabled", isHeadlessTestDisabled());
        WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                .configure(ChromeUnitTests.createChromeOptions())
                .configure(options -> {
                    options.setAcceptInsecureCerts(true);
                })
                .configure(o -> o.setHeadless(true))
                .environment(createEnvironmentSupplierForDisplay(headless))
                .build();
        testTrafficCollectorOnHttpbin(webDriverFactory);
    }

    @Override
    protected boolean isHeadlessTestDisabled() {
        return ChromeUnitTests.isHeadlessChromeTestsDisabled();
    }
}
