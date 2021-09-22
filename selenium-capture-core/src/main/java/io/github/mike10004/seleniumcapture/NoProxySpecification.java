package io.github.mike10004.seleniumcapture;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

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
        public UpstreamProxy createUpstreamProxy() {
            return null;
        }

        @Override
        public List<String> getProxyBypassList() {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return "UpstreamProxyDefinition{NO_PROXY}";
        }
    }
}
