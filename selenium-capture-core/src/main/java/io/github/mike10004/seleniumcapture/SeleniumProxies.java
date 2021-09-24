package io.github.mike10004.seleniumcapture;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Static utility methods relating to Selenium proxy instances.
 */
public class SeleniumProxies {

    /**
     * Defined by {@link org.openqa.selenium.Proxy#setNoProxy(String)}.
     */
    private static final String NONPROXY_HOST_PATTERN_DELIMITER = ",";

    private SeleniumProxies() {}

    /**
     * Transforms a list of proxy bypass patterns, as specified in a {@link ProxyDefinition},
     * into a string suitable for {@link org.openqa.selenium.Proxy#setNoProxy(String)}.
     * <p>
     * Unfortunately the rule syntax is browser-dependent. To be safe, use the limited
     * subset of rules that Firefox and Chrome both support, including literal hostnames
     * or domains, IP address literals, and CIDR blocks.
     * Chromium's bypass rule syntax is described here: https://chromium.googlesource.com/chromium/src/+/HEAD/net/docs/proxy.md#Proxy-bypass-rules
     * An official description of Firefox's bypass rule syntax is hard to come by, but some descriptions are:
     *    * https://support.mozilla.org/en-US/kb/connection-settings-firefox (minimal, end-user-oriented)
     *    * http://kb.mozillazine.org/No_proxy_for (unofficial, technical and complete, possibly out of date)
     *    * https://superuser.com/questions/281229/how-to-specify-wildcards-in-proxy-exceptions-in-firefox (unofficial, incomplete)
     * @param bypassPatterns
     * @return
     */
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
