package com.github.mike10004.seleniumhelp;

import org.openqa.selenium.Proxy;

import javax.annotation.Nullable;

class NoProxySpecification implements ProxySpecification {

    private static final ProxySpecification INSTANCE = new NoProxySpecification();

    private NoProxySpecification() {}

    public static ProxySpecification getInstance() {
        return INSTANCE;
    }

    @Nullable
    @Override
    public UpstreamProxy toUpstreamProxy() {
        return null;
    }

    @Override
    public Proxy createSeleniumProxy() {
        return null;
    }

}
