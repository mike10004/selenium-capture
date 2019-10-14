package com.github.mike10004.seleniumhelp;

import javax.annotation.Nullable;

public interface ProxySpecification {

    org.openqa.selenium.Proxy createSeleniumProxy();

    @Nullable
    UpstreamProxy toUpstreamProxy();

    static ProxySpecification noProxy() {
        return NoProxySpecification.getInstance();
    }
}

