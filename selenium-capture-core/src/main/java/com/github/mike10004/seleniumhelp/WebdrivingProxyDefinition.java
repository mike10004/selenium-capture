package com.github.mike10004.seleniumhelp;

import javax.annotation.Nullable;

public interface WebdrivingProxyDefinition {

    @Nullable
    org.openqa.selenium.Proxy createWebdrivingProxy();

}
