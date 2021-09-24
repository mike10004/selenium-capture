package io.github.mike10004.seleniumcapture;

import com.google.common.base.MoreObjects;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;

import java.net.URI;

class HostnameWildcardBypassRule implements HostBypassRule {

    private final String pattern;

    public HostnameWildcardBypassRule(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean isBypass(URI httpRequestUri) {
        String uriHost = httpRequestUri.getHost();
        if (uriHost != null) {
            return FilenameUtils.wildcardMatch(uriHost, pattern, IOCase.INSENSITIVE);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("pattern", pattern)
                .toString();
    }
}
