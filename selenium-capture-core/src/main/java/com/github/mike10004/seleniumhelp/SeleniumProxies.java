package com.github.mike10004.seleniumhelp;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SeleniumProxies {

    /**
     * Defined by {@link org.openqa.selenium.Proxy#setNoProxy(String)}.
     */
    private static final String NONPROXY_HOST_PATTERN_DELIMITER = ",";

    private SeleniumProxies() {}

    @Nullable
    public static String makeNoProxyValue(List<String> bypassPatterns) {
        String joinedBypassPatterns = bypassPatterns.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.joining(SeleniumProxies.NONPROXY_HOST_PATTERN_DELIMITER));
        return (Strings.emptyToNull(joinedBypassPatterns));
    }

    public static List<String> getProxyBypasses(@Nullable org.openqa.selenium.Proxy proxy) {
        if (proxy == null) {
            return Collections.emptyList();
        }
        String noProxyValue = proxy.getNoProxy();
        if (Strings.isNullOrEmpty(noProxyValue)) {
            return Collections.emptyList();
        }
        return Splitter.on(NONPROXY_HOST_PATTERN_DELIMITER).omitEmptyStrings().trimResults().splitToList(noProxyValue);
    }
}
