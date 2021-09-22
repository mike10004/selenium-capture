package io.github.mike10004.seleniumcapture;

import javax.annotation.Nullable;
import java.util.List;

interface UpstreamProxyDefinition {

    @Nullable
    UpstreamProxy createUpstreamProxy();

    List<String> getProxyBypassList();

}
