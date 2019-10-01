package com.github.mike10004.seleniumhelp;

import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
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
import java.util.function.Function;
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

    @Nullable
    public static UpstreamProxy toUpstreamProxy(URI uri) {
        if (uri == null) {
            return null;
        }
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
    public static org.openqa.selenium.Proxy createSeleniumProxy(URI proxySpecification, Function<List<String>, List<String>> bypassListPopulator) {
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
        List<String> bypassParameterValues = getProxyBypassesFromQueryString(proxySpecification);
        List<String> bypassPatterns = bypassListPopulator.apply(bypassParameterValues);
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

    @Nullable
    public static URI createSimple(@Nullable HostAndPort socketAddress, ChainedProxyType proxyType) {
        if (socketAddress == null) {
            return null;
        }
        try {
            return new URIBuilder()
                    .setHost(socketAddress.getHost())
                    .setPort(socketAddress.getPort())
                    .setScheme(proxyType.name().toLowerCase())
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Interface of a service class that performs configuration operations relating to proxy instances.
     */
    interface BmpConfigurator {

        /**
         * Configures the chained proxy manager of a proxy instance.
         * @param proxy the proxy to configure
         */
        void configureUpstream(BrowserMobProxy proxy);

        /**
         * Creates a set of webdriving configuration values that are appropriate for the upstream proxy configuration.
         * @param bmp the proxy
         * @param certificateAndKeySource the certificate and key source
         * @return a new webdriving config instance
         */
        WebdrivingConfig createWebdrivingConfig(BrowserMobProxy bmp, @Nullable CertificateAndKeySource certificateAndKeySource);

        /**
         * Returns a configurator that configures a direct connection upstream, meaning no proxy is to be used.
         * @return a configurator
         */
        static BmpConfigurator noProxy() {
            return new BmpConfigurator() {
                @Override
                public void configureUpstream(BrowserMobProxy bmp) {
                    bmp.setChainedProxy(null);
                    if (bmp instanceof BrowserMobProxyServer) {
                        ((BrowserMobProxyServer)bmp).setChainedProxyManager(null);
                    }
                }

                @Override
                public WebdrivingConfig createWebdrivingConfig(BrowserMobProxy bmp, @Nullable CertificateAndKeySource certificateAndKeySource) {
                    return WebdrivingConfig.builder()
                            .proxy(BrowserMobs.getConnectableSocketAddress(bmp))
                            .certificateAndKeySource(certificateAndKeySource)
                            .build();
                }

                @Override
                public String toString() {
                    return "TrafficCollectorProxyConfigurator{UPSTREAM_NOPROXY}";
                }
            };
        }

        /**
         * Returns a configurator that does not act upon a proxy instance.
         * @return a configurator instance
         */
        static BmpConfigurator inoperative() {
            return new BmpConfigurator() {
                @Override
                public void configureUpstream(BrowserMobProxy proxy) {
                }

                @Override
                public WebdrivingConfig createWebdrivingConfig(BrowserMobProxy bmp, @Nullable CertificateAndKeySource certificateAndKeySource) {
                    return WebdrivingConfig.builder()
                            .proxy(BrowserMobs.getConnectableSocketAddress(bmp))
                            .certificateAndKeySource(certificateAndKeySource)
                            .build();
                }

                @Override
                public String toString() {
                    return "TrafficCollectorProxyConfigurator{INOPERATIVE}";
                }
            };
        }

        /**
         * Returns a configurator that configures a proxy to use an upstream proxy specified by a URI.
         * @param proxySpecUriProvider the supplier of the URI
         * @return a configurator instance
         */
        static BmpConfigurator upstream(Supplier<URI> proxySpecUriProvider) {
            requireNonNull(proxySpecUriProvider);
            return new BmpConfigurator() {
                @Override
                public void configureUpstream(BrowserMobProxy bmp) {
                    @Nullable URI proxySpecUri = proxySpecUriProvider.get();
                    if (proxySpecUri == null) {
                        noProxy().configureUpstream(bmp);
                    } else {
                        ChainedProxyManager chainedProxyManager = toUpstreamProxy(proxySpecUri);
                        ((BrowserMobProxyServer)bmp).setChainedProxyManager(chainedProxyManager);
                    }
                }

                @Override
                public WebdrivingConfig createWebdrivingConfig(BrowserMobProxy bmp, @Nullable CertificateAndKeySource certificateAndKeySource) {
                    URI proxySpecUri = proxySpecUriProvider.get();
                    List<String> hostBypassPatterns = getProxyBypassesFromQueryString(proxySpecUri);
                    return WebdrivingConfig.builder()
                            .proxy(BrowserMobs.getConnectableSocketAddress(bmp), hostBypassPatterns)
                            .certificateAndKeySource(certificateAndKeySource)
                            .build();
                }
            };
        }

    }

    /**
     * Constructs a URI based on a given URI but with an additional proxy host bypass pattern.
     * @param proxyUri the original proxy URI
     * @param hostPattern the new host bypass pattern
     * @return the new URI
     */
    public static URI addBypass(URI proxyUri, String hostPattern) {
        requireNonNull(hostPattern, "hostPattern");
        return addBypasses(proxyUri, Collections.singleton(hostPattern));
    }

    /**
     * Constructs a URI with host bypass patterns. The new URI is based on the old URI.
     * @param proxyUri the original proxy URI
     * @param bypasses a collection of host bypass patterns
     * @return the new URI
     */
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
