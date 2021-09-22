package io.github.mike10004.seleniumcapture;

import javax.annotation.Nullable;

final class NoProxySpecification {

    private static final UpstreamProxyDefinition upstream = new NoUpstreamProxyDefinition();

    private NoProxySpecification() {}

    public static UpstreamProxyDefinition noUpstreamProxyDefinition() {
        return upstream;
    }

    private static class NoUpstreamProxyDefinition implements UpstreamProxyDefinition {

        public NoUpstreamProxyDefinition() {}

        @Nullable
        @Override
        public UpstreamProxyManager createUpstreamProxy() {
            return null;
        }

        @Override
        public String toString() {
            return "UpstreamProxyDefinition{NO_PROXY}";
        }
    }
}
