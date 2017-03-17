package com.github.mike10004.seleniumhelp;

/**
 * Static constants and utility methods to assist with tests.
 */
class UnitTests {

    public static final String RECOMMENDED_CHROMEDRIVER_VERSION = "2.27";

    private UnitTests() {}

    /**
     * Required version for the geckodriver. As of 2017-03-10, there's something
     * kooky about 0.15.0, so we have to peg this at 0.14.0.
     */
    public static final String REQUIRED_GECKODRIVER_VERSION = "0.14.0";
}
