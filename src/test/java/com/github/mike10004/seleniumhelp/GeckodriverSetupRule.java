package com.github.mike10004.seleniumhelp;

import org.junit.rules.ExternalResource;

/**
 * Use this as a {@link org.junit.ClassRule}.
 */
public class GeckodriverSetupRule extends ExternalResource {

    private static volatile boolean setupInvoked;

    @Override
    protected void before() {
        if (!setupInvoked) {
            UnitTests.setupRecommendedGeckoDriver();
        }
        setupInvoked = true;
    }
}
