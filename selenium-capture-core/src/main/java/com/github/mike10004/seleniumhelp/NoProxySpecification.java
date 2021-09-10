package com.github.mike10004.seleniumhelp;

import org.openqa.selenium.Proxy;

import javax.annotation.Nullable;

final class NoProxySpecification {

    private static final NoProxySpecification INSTANCE = new NoProxySpecification();

    private final WebdrivingProxyDefinition webdriving = new NoWebdrivingProxyDefinition();

    private final UpstreamProxyDefinition upstream = new NoUpstreamProxyDefinition();

    private NoProxySpecification() {}

    public static NoProxySpecification getInstance() {
        return INSTANCE;
    }

    public WebdrivingProxyDefinition asWebdrivingProxyDefinition() {
        return webdriving;
    }

    public UpstreamProxyDefinition asUpstreamProxyDefinition() {
        return upstream;
    }

    @Override
    public String toString() {
        return "NoProxySpecification{}";
    }

    private static class NoWebdrivingProxyDefinition implements WebdrivingProxyDefinition {

        @Nullable
        @Override
        public Proxy createWebdrivingProxy() {
            return new org.openqa.selenium.Proxy().setProxyType(org.openqa.selenium.Proxy.ProxyType.DIRECT);
        }

        @Override
        public String toString() {
            return "WebdrivingProxyDefinition{DIRECT}";
        }
    }

    private static class NoUpstreamProxyDefinition implements UpstreamProxyDefinition {

        @Nullable
        @Override
        public UpstreamProxy createUpstreamProxy() {
            return null;
        }

        @Override
        public String toString() {
            return "UpstreamProxyDefinition{NO_PROXY}";
        }
    }
}
