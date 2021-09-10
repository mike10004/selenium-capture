package io.github.mike10004.seleniumcapture.firefox;

import com.github.mike10004.seleniumhelp.WebDriverFactory;
import com.github.mike10004.xvfbtesting.XvfbRule;
import io.github.bonigarcia.wdm.config.DriverManagerType;
import io.github.mike10004.seleniumcapture.testbases.WebDriverTestParameter;

import java.net.URI;

public     class FirefoxTestParameter implements WebDriverTestParameter {

    private final boolean acceptInsecureCerts;

    public FirefoxTestParameter() {
        this(false);
    }

    public FirefoxTestParameter(boolean acceptInsecureCerts) {
        this.acceptInsecureCerts = acceptInsecureCerts;
    }

    @Override
    public WebDriverFactory createWebDriverFactory(XvfbRule xvfb) {
        return FirefoxWebDriverFactory.builder()
                .binary(FirefoxUnitTests.createFirefoxBinarySupplier())
                .putPreferences(FirefoxUnitTests.createFirefoxPreferences())
                .acceptInsecureCerts(acceptInsecureCerts)
                .environment(xvfb.getController().newEnvironment())
                .build();
    }

    /**
     * Firefox does not send 'br' in the accept-encoding request header for http requests,
     * and (I suppose) doesn't decode responses in encodings it didn't expect.
     * See: https://bugzilla.mozilla.org/show_bug.cgi?id=1218924
     * @return false if URL scheme is http
     */
    @Override
    public boolean isBrotliSupported(String url) {
        return !"http".equals(URI.create(url).getScheme());
    }

    @Override
    public DriverManagerType getDriverManagerType() {
        return DriverManagerType.FIREFOX;
    }

}

