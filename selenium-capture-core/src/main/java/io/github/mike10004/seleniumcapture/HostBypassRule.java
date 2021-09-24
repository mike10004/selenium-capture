package io.github.mike10004.seleniumcapture;

import java.net.URI;

public interface HostBypassRule {

    boolean isBypass(URI httpRequestUri);

}
