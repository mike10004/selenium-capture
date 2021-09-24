package io.github.mike10004.seleniumcapture;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.net.HostAndPort;
import com.google.common.net.InternetDomainName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressSeqRange;
import inet.ipaddr.IPAddressString;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.net.URI;
import java.util.List;

/**
 * Default implementation of a host bypass rule factory.
 * Only a few rule types are handled: IPv4 or IPv6 address literals
 * (with or without port), IPv4 CIDR blocks, hostname literals, and
 * hostname wildcard patterns.
 *
 * Hostname wildcard patterns must start with *. and not contain any
 * other wildcard characters. Firefox-style hostname patterns are accepted
 * where the lead character is a dot and is interpretd as '*.'. For example
 * {@code .google.com} is interpreted as {@code *.google.com}.
 */
class DefaultHostBypassRuleFactory implements HostBypassRuleFactory {

    public DefaultHostBypassRuleFactory() {}

    @Override
    public HostBypassRule fromSpec(String bypassSpec) {
        if (isIpAddressLiteral(bypassSpec)) {
            return new HostnameLiteralBypassRule(HostAndPort.fromString(bypassSpec));
        }
        if (isHostnameLiteral(bypassSpec)) {
            return new HostnameLiteralBypassRule(HostAndPort.fromString(bypassSpec));
        }
        if (isIpv4CidrBlock(bypassSpec)) {
            return new CidrBlockBypassRule(bypassSpec);
        }
        if (isHostPattern(bypassSpec)) {
            return new HostnameWildcardBypassRule(bypassSpec);
        }
        if (isHostPattern("*" + bypassSpec)) {
            return new HostnameWildcardBypassRule("*" + bypassSpec);
        }
        return new IgnoreRule(bypassSpec);
    }

    private static class IgnoreRule implements HostBypassRule {

        private final String ruleSpec;

        public IgnoreRule(String ruleSpec) {
            this.ruleSpec = ruleSpec;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper("IgnoringBypassRule")
                    .add("ruleSpec", StringUtils.abbreviate(ruleSpec, 512))
                    .toString();
        }

        @Override
        public boolean isBypass(URI httpRequestUri) {
            return false;
        }
    }

    static boolean isIpAddressLiteral(String hostPattern) {
        try {
            hostPattern = HostAndPort.fromString(hostPattern).getHost();
        } catch (RuntimeException ignore) {
            return false;
        }
        InetAddressValidator validator = InetAddressValidator.getInstance();
        boolean valid = validator.isValid(hostPattern);
        if (valid) {
            return true;
        }
        // validator doesn't handle IPv6 in brackets; if this is a valid address
        // bookended by brackets, then we want to say so
        if (hostPattern.length() >= 2) {
            if (hostPattern.charAt(0) == '[' && hostPattern.charAt(hostPattern.length() - 1) == ']') {
                hostPattern = hostPattern.substring(1, hostPattern.length() - 2);
                return validator.isValidInet6Address(hostPattern);
            }
        }
        return false;
    }

    static boolean isIpv4CidrBlock(String hostPattern) {
        if (isIpAddressLiteral(hostPattern)) {
            return false;
        }
        try {
            IPAddressSeqRange range = new IPAddressString(hostPattern).getSequentialRange();
            IPAddress prefixBlock = range.coverWithPrefixBlock();
            return range.getCount().equals(prefixBlock.getCount());
        } catch (RuntimeException ignore) {
        }
        return false;
    }

    static boolean isHostnameLiteral(String token) {
        try {
            token = HostAndPort.fromString(token).getHost();
        } catch (RuntimeException ignore) {
            return false;
        }
        try {
            InternetDomainName name = InternetDomainName.from(token);
            CharMatcher dot = CharMatcher.is('.');
            List<String> parts = name.parts();
            return parts.stream().allMatch(InternetDomainName::isValid)
                    && dot.countIn(token) + 1 == parts.size();
        } catch (IllegalArgumentException ignore) {
            return false;
        }
    }

    /**
     * Checks whether this is a host pattern with a wildcard prefix.
     *
     * @param hostPattern
     * @return
     */
    static boolean isHostPattern(String hostPattern) {
        String prefix = "*.";
        if (hostPattern.startsWith(prefix)) {
            String remainder = hostPattern.substring(prefix.length());
            return InternetDomainName.isValid(remainder);
        }
        return false;
    }

}
