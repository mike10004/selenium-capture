package io.github.mike10004.seleniumcapture;

import javax.annotation.Nullable;

public interface UpstreamProxyDefinition {

    @Nullable
    UpstreamProxyManager createUpstreamProxy();

}
