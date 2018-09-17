package com.github.mike10004.seleniumhelp;

import com.google.common.base.Strings;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.littleshoot.proxy.ChainedProxyManager;
import org.littleshoot.proxy.ChainedProxyType;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

class ProxyUris {

    public static final String PARAM_BYPASS = "bypass";

    private ProxyUris() {}

    static List<String> getProxyBypassesFromQueryString(@Nullable URI proxySpecification) {
        if (proxySpecification != null) {
            List<NameValuePair> queryParams = URLEncodedUtils.parse(proxySpecification, StandardCharsets.UTF_8);
            return queryParams.stream()
                    .filter(param -> PARAM_BYPASS.equalsIgnoreCase(param.getName()))
                    .map(NameValuePair::getValue)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public static String getUsername(URI uri) {
        return getCredentials(uri)[0];
    }

    public static String getPassword(URI uri) {
        return getCredentials(uri)[1];
    }

    private static String[] getCredentials(URI uri) {
        String userInfo = uri.getUserInfo();
        if (!Strings.isNullOrEmpty(userInfo)) {
            String[] parts = userInfo.split(":", 2);
            return parts.length == 2 ? parts : new String[]{parts[0], null};
        }
        return new String[]{null, null};
    }

    public static UpstreamProxy toUpstreamProxy(URI uri) {
        ChainedProxyType type = ChainedProxyType.HTTP;
        if (isSocks(uri)) {
            @Nullable Integer socksVersion = parseSocksVersionFromUriScheme(uri);
            if (Integer.valueOf(4).equals(socksVersion)) {
                type = ChainedProxyType.SOCKS4;
            } else {
                type = ChainedProxyType.SOCKS5;
            }
        }
        String username = getUsername(uri), password = getPassword(uri);
        return new UpstreamProxy(type, uri.getHost(), uri.getPort(), username, password);
    }

    public static boolean isSocks(URI proxySpecification) {
        String scheme = proxySpecification.getScheme();
        return "socks".equalsIgnoreCase(scheme) || "socks4".equalsIgnoreCase(scheme) || "socks5".equalsIgnoreCase(scheme);
    }

    @Nullable
    public static Integer parseSocksVersionFromUriScheme(URI uri) {
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

    /**
     * Creates a Selenium Proxy object using the specified socket address as the HTTP proxy server.
     * @param proxySpecification URI specifying the proxy; see {@link WebdrivingConfig#getProxySpecification()}
     * @return a Selenium Proxy instance, configured to use the specified address and port as its proxy server
     * @author {@link net.lightbody.bmp.client.ClientUtil}
     */
    @Nullable
    public static org.openqa.selenium.Proxy createSeleniumProxy(URI proxySpecification) {
        if (proxySpecification == null) {
            return null;
        }
        org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();
        proxy.setProxyType(org.openqa.selenium.Proxy.ProxyType.MANUAL);
        String socketAddress = String.format("%s:%d", proxySpecification.getHost(), proxySpecification.getPort());
        String userInfo = proxySpecification.getUserInfo();
        if (isSocks(proxySpecification)) {
            proxy.setSocksProxy(socketAddress);
            proxy.setSocksVersion(parseSocksVersionFromUriScheme(proxySpecification));
            proxy.setSocksUsername(getUsername(proxySpecification));
            proxy.setSocksPassword(getPassword(proxySpecification));
        } else {
            if (!Strings.isNullOrEmpty(userInfo)) {
                LoggerFactory.getLogger(ProxyUris.class).warn("HTTP proxy server credentials may not be specified in the proxy specification URI (and I'm not sure what to suggest instead); only SOCKS proxy server credentials may be specified in the proxy specification URI");
            }
            proxy.setHttpProxy(socketAddress);
            proxy.setSslProxy(socketAddress);
        }
        List<String> bypassPatterns = getProxyBypassesFromQueryString(proxySpecification);
        String joinedBypassPatterns = bypassPatterns.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.joining(NONPROXY_HOST_PATTERN_DELIMITER));
        proxy.setNoProxy(Strings.emptyToNull(joinedBypassPatterns));
        return proxy;
    }

    /**
     * Defined by {@link org.openqa.selenium.Proxy#setNoProxy(String)}.
     */
    private static final String NONPROXY_HOST_PATTERN_DELIMITER = ",";

    interface BmpConfigurator {

        static BmpConfigurator noProxy() {
            return bmp -> {
                bmp.setChainedProxy(null);
                if (bmp instanceof BrowserMobProxyServer) {
                    ((BrowserMobProxyServer)bmp).setChainedProxyManager(null);
                }
            };
        }

        static BmpConfigurator fromUriSupplier(Supplier<URI> proxySpecificationSupplier) {
            return upstream(() -> {
                URI proxySpec = proxySpecificationSupplier.get();
                return toUpstreamProxy(proxySpec);
            });
        }

        void configure(BrowserMobProxy proxy);

        static BmpConfigurator inoperative() {
            return proxy -> {};
        }

        static BmpConfigurator upstream(Supplier<ChainedProxyManager> chainedProxyManagerSupplier) {
            requireNonNull(chainedProxyManagerSupplier);
            return bmp -> {
                @Nullable ChainedProxyManager chainedProxyManager = chainedProxyManagerSupplier.get();
                if (chainedProxyManager == null) {
                    noProxy().configure(bmp);
                } else {
                    ((BrowserMobProxyServer)bmp).setChainedProxyManager(chainedProxyManager);
                }
            };
        }

    }

    public static URI addBypass(URI proxyUri, String hostPattern) {
        return addBypasses(proxyUri, Collections.singleton(hostPattern));
    }

    public static URI addBypasses(URI proxyUri, Collection<String> bypasses) {
        if (bypasses.isEmpty()) {
            return proxyUri;
        }
        try {
            URIBuilder b = new URIBuilder(proxyUri);
            bypasses.forEach(hostPattern -> b.addParameter(ProxyUris.PARAM_BYPASS, hostPattern));
            return b.build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

    }
}
