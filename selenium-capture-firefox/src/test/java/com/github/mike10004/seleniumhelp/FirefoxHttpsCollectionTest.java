package com.github.mike10004.seleniumhelp;

import io.github.mike10004.seleniumcapture.testbases.CollectionTestBase;
import org.junit.Assume;
import org.junit.Test;

public class FirefoxHttpsCollectionTest extends CollectionTestBase {

    public FirefoxHttpsCollectionTest() {
        super(new FirefoxTestParameter(), "https");
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
        if (headless) {
            Assume.assumeFalse("headless tests disabled", isHeadlessTestDisabled());
        }
        WebDriverFactory webDriverFactory = FirefoxWebDriverFactory.builder()
                .binary(FirefoxUnitTests.createFirefoxBinarySupplier())
                .environment(createEnvironmentSupplierForDisplay(headless))
                .acceptInsecureCerts()
                .configure(o -> o.setHeadless(headless))
                .build();
        testTrafficCollectorOnHttpbin(webDriverFactory);
    }

    @Override
    protected boolean isHeadlessTestDisabled() {
        return false;
    }
}
