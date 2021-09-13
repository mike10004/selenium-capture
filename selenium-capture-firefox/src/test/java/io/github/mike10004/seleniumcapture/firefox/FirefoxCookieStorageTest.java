package io.github.mike10004.seleniumcapture.firefox;

import io.github.mike10004.seleniumcapture.testbases.CookieStorageTestBase;

public class FirefoxCookieStorageTest extends CookieStorageTestBase {

    public FirefoxCookieStorageTest() {
        super(new FirefoxTestParameter(true));
    }

}
