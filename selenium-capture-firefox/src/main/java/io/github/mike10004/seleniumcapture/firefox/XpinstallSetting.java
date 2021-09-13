package io.github.mike10004.seleniumcapture.firefox;

import org.openqa.selenium.firefox.FirefoxProfile;

/**
 * Enumeration of constants that represent ways to set
 * a Firefox preference relating to extension installs.
 */
public enum XpinstallSetting implements FirefoxWebDriverFactory.FirefoxProfileAction {
    NOT_MODIFIED,
    SIGNATURE_REQUIRED_TRUE,
    SIGNATURE_REQUIRED_FALSE;

    private static final String PREF_KEY = "xpinstall.signatures.required";

    @Override
    public void perform(FirefoxProfile profile) {
        switch (this) {
            case NOT_MODIFIED:
                break;
            case SIGNATURE_REQUIRED_FALSE:
                profile.setPreference(PREF_KEY, false);
                break;
            case SIGNATURE_REQUIRED_TRUE:
                profile.setPreference(PREF_KEY, true);
                break;
        }
    }

}
