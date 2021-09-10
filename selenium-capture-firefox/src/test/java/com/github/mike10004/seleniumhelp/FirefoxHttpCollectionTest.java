package com.github.mike10004.seleniumhelp;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class FirefoxHttpCollectionTest extends CollectionTestBase {

    public FirefoxHttpCollectionTest() {
        super(new FirefoxTestParameter(false), "http");
    }

    @Test
    public void http() throws Exception {
        testhttp(false);
    }

    @Test
    public void http_headless() throws Exception {
        testhttp(true);
    }

    protected void testhttp(boolean headless) throws Exception {
        if (headless) {
            Assume.assumeFalse("headless tests disabled", isHeadlessTestDisabled());
        }
        WebDriverFactory webDriverFactory = FirefoxWebDriverFactory.builder()
                .binary(FirefoxUnitTests.createFirefoxBinarySupplier())
                .configure(o -> o.setHeadless(headless))
                .environment(createEnvironmentSupplierForDisplay(headless))
                .build();
        testTrafficCollectorOnExampleDotCom(webDriverFactory);
    }

    @Override
    protected boolean isHeadlessTestDisabled() {
        return false;
    }

}
