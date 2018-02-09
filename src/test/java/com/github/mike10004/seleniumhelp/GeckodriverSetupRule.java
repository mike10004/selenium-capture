package com.github.mike10004.seleniumhelp;

import org.junit.rules.ExternalResource;

/**
 * Rule that downloads geckodriver and sets the system properties that specify
 * the geckodriver path. Use this as a {@link org.junit.ClassRule}.
 */
public class GeckodriverSetupRule extends ExternalResource {

    private static final Object setupLock = new Object();
    private static volatile boolean setupInvoked;

    @Override
    protected void before() {
        synchronized (setupLock) {
            if (!setupInvoked) {
                UnitTests.setupRecommendedGeckoDriver();
            }
            setupInvoked = true;
        }
    }
}
