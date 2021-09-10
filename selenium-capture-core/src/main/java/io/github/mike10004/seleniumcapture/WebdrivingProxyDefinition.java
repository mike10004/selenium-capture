package io.github.mike10004.seleniumcapture;

import javax.annotation.Nullable;

public interface WebdrivingProxyDefinition {

    /**
     * Returns the selenium proxy.
     * May NOT return null; use a proxy with type DIRECT instead.
     * @return proxy; never null
     */
    org.openqa.selenium.Proxy createWebdrivingProxy();

}
