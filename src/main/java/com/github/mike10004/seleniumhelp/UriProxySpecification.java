package com.github.mike10004.seleniumhelp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.littleshoot.proxy.ChainedProxyType;

import javax.annotation.Nullable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class UriProxySpecification extends ProxySpecificationBase {

    public static final String PARAM_BYPASS = "bypass";

    private final URI uri;

    private UriProxySpecification(@Nullable URI uri) {
        this.uri = requireNonNull(uri);
    }

    /**
     * Creates a specification from a URI. The proxy host and port
     * must be specified by the URI, and the URI may additionally specify credentials with
     * {@link URI#getUserInfo()} and type, SOCKS or HTTP, with the URI scheme. If the scheme
     * does not specify the type, then it is assumed that the proxy is an HTTP proxy server.
     * Patterns of hosts to bypass may be supplied as values of query parameters with name
     * {@link #PARAM_BYPASS}.
     * * @param uri
     * @return
     */
    public static ProxySpecification of(@Nullable URI uri) {
        if (uri == null) {
            return ProxySpecification.noProxy();
        }
        return new UriProxySpecification(uri);
    }

    @Override
    public List<String> getProxyBypasses() {
        if (uri != null) {
            List<NameValuePair> queryParams = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
            return queryParams.stream()
                    .filter(param -> PARAM_BYPASS.equalsIgnoreCase(param.getName()))
                    .map(NameValuePair::getValue)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public String getUsername() {
        return getCredentials(uri)[0];
    }

    @Override
    public String getPassword() {
        return getCredentials(uri)[1];
    }

    private static String[] getCredentials(@Nullable URI uri) {
        if (uri != null) {
            String userInfo = uri.getUserInfo();
            if (!Strings.isNullOrEmpty(userInfo)) {
                String[] parts = userInfo.split(":", 2);
                return parts.length == 2 ? parts : new String[]{parts[0], null};
            }
        }
        return new String[]{null, null};
    }

    @Override
    @Nullable
    public UpstreamProxy toUpstreamProxy() {
        if (uri == null) {
            return null;
        }
        ChainedProxyType type = ChainedProxyType.HTTP;
        if (isSocks()) {
            @Nullable Integer socksVersion = parseSocksVersionFromUriScheme(uri);
            if (Integer.valueOf(4).equals(socksVersion)) {
                type = ChainedProxyType.SOCKS4;
            } else {
                type = ChainedProxyType.SOCKS5;
            }
        }
        String username = getUsername(), password = getPassword();
        return new UpstreamProxy(type, uri.getHost(), uri.getPort(), username, password);
    }

    @Override
    public boolean isSocks() {
        if (uri == null) {
            return false;
        }
        String scheme = uri.getScheme();
        return "socks".equalsIgnoreCase(scheme) || "socks4".equalsIgnoreCase(scheme) || "socks5".equalsIgnoreCase(scheme);
    }

    @VisibleForTesting
    @Nullable
    static Integer parseSocksVersionFromUriScheme(URI uri) {
        String uriScheme = uri.getScheme();
        if ("socks4".equalsIgnoreCase(uriScheme)) {
            return 4;
        }
        if ("socks5".equalsIgnoreCase(uriScheme)) {
            return 5;
        }
        return null;
    }

    @Nullable
    public static String toScheme(@Nullable ChainedProxyType upstreamProxyType) {
        if (upstreamProxyType == null) {
            return null;
        }
        return upstreamProxyType.name().toLowerCase();
    }

    @Override
    public String toString() {
        return uri == null ? "AbsentProxySpecification{}" : uri.toString();
    }

    @Nullable
    @Override
    public Integer getSocksVersion() {
        return uri == null ? null : parseSocksVersionFromUriScheme(uri);
    }

    @Nullable
    @Override
    public FullSocketAddress getSocketAddress() {
        if (uri == null) {
            return null;
        }
        return FullSocketAddress.define(uri.getHost(), uri.getPort());
    }

    public URI getUri() {
        return uri;
    }
}
