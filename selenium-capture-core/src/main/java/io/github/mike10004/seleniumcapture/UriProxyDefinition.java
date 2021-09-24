package io.github.mike10004.seleniumcapture;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.littleshoot.proxy.ChainedProxyManager;
import org.littleshoot.proxy.ChainedProxyType;
import org.openqa.selenium.Proxy;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

class UriProxyDefinition implements ProxyDefinition {

    static final String PARAM_BYPASS = "bypass";

    private final URI uri;

    private UriProxyDefinition(URI uri) {
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
    public static UriProxyDefinition of(URI uri) {
        return new UriProxyDefinition(uri);
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

    @Nullable
    private static String getUsername(@Nullable URI uri) {
        return getCredentials(uri)[0];
    }

    @Nullable
    private static String getPassword(@Nullable URI uri) {
        return getCredentials(uri)[1];
    }

    private static boolean isSocks(@Nullable URI uri) {
        if (uri == null) {
            return false;
        }
        String scheme = uri.getScheme();
        return "socks".equalsIgnoreCase(scheme) || "socks4".equalsIgnoreCase(scheme) || "socks5".equalsIgnoreCase(scheme);
    }

    public static ProxyDefinition noProxy() {
        return new NoProxyDefinition();
    }

    @Override
    public void configureUpstreamProxy(BrowserUpProxy browserUpProxy, HostBypassRuleFactory bypassRuleFactory) {
        Preconditions.checkArgument(browserUpProxy instanceof BrowserUpProxyServer, "argument must be instance of " + BrowserUpProxyServer.class);
        ChainedProxyManager chainedProxyManager = new UpstreamProxyProvider(uri).createUpstreamProxy(bypassRuleFactory);
        ((BrowserUpProxyServer)browserUpProxy).setChainedProxyManager(chainedProxyManager);
    }

    private static class UpstreamProxyProvider  {

        private final URI uri;

        public UpstreamProxyProvider(URI uri) {
            this.uri = requireNonNull(uri);
        }

        @Override
        public String toString() {
            return "UpstreamProxyDefinition{uri=" + uri + "}";
        }

        @Nullable
        public ChainedProxyManager createUpstreamProxy(HostBypassRuleFactory bypassRuleFactory) {
            // default to HTTP, if URI has no scheme
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
            List<HostBypassRule> bypassRules = getProxyBypasses(uri).stream()
                    .map(bypassRuleFactory::fromSpec)
                    .collect(Collectors.toList());
            return new UpstreamProxyManager(type, uri.getHost(), uri.getPort(), username, password, new ListHostBypassPredicate(bypassRules));
        }
    }

    @Override
    public org.openqa.selenium.Proxy createWebdrivingProxy() {
        return new WebdrivingProxyProvider().createWebdrivingProxy();
    }

    private class WebdrivingProxyProvider extends SeleniumProxyCreationAssistant {

        public WebdrivingProxyProvider() {
        }

        @Override
        public String toString() {
            String token = uri == null ? "ABSENT" : uri.toString();
            return "WebdrivingProxyDefinition{uri=" + token + "}";
        }

        public Proxy createWebdrivingProxy() {
            return createSeleniumProxy();
        }

        @Override
        public String getUsername() {
            return UriProxyDefinition.getUsername(uri);
        }

        @Override
        public String getPassword() {
            return UriProxyDefinition.getPassword(uri);
        }

        @Override
        public List<String> getProxyBypasses() {
            return UriProxyDefinition.getProxyBypasses(uri);
        }

        @Override
        public boolean isSocks() {
            return UriProxyDefinition.isSocks(uri);
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

    }

    private static List<String> getProxyBypasses(URI uri) {
        if (uri != null) {
            List<NameValuePair> queryParams = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
            return queryParams.stream()
                    .filter(param -> PARAM_BYPASS.equalsIgnoreCase(param.getName()))
                    .map(NameValuePair::getValue)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
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

    @Override
    public String toString() {
        return "UriProxyDefinition{uri=" + uri + "}";
    }

    public URI getUri() {
        return uri;
    }

    private abstract static class SeleniumProxyCreationAssistant {

        protected abstract List<String> getProxyBypasses();

        @Nullable
        protected abstract String getUsername();

        @Nullable
        protected abstract String getPassword();

        protected abstract boolean isSocks();

        @Nullable
        protected abstract Integer getSocksVersion();

        @Nullable
        protected abstract FullSocketAddress getSocketAddress();

        protected final Proxy createSeleniumProxy() {
            @Nullable FullSocketAddress hostAndPort = getSocketAddress();
            if (hostAndPort == null) {
                return null;
            }
            Proxy proxy = new Proxy();
            proxy.setProxyType(Proxy.ProxyType.MANUAL);
            String socketAddress = String.format("%s:%d", hostAndPort.getHost(), hostAndPort.getPort());
            @Nullable String userInfo = getUsername();
            boolean socks = isSocks();
            if (socks) {
                proxy.setSocksProxy(socketAddress);
                proxy.setSocksVersion(getSocksVersion());
                proxy.setSocksUsername(getUsername());
                proxy.setSocksPassword(getPassword());
            } else {
                if (!Strings.isNullOrEmpty(userInfo)) {
                    LoggerFactory.getLogger(getClass()).warn("HTTP proxy server credentials may not be specified in the proxy specification URI (and I'm not sure what to suggest instead); only SOCKS proxy server credentials may be specified in the proxy specification URI");
                }
                proxy.setHttpProxy(socketAddress);
                proxy.setSslProxy(socketAddress);
            }
            List<String> bypassPatterns = getProxyBypasses();
            proxy.setNoProxy(SeleniumProxies.makeNoProxyValue(bypassPatterns));
            return proxy;
        }

    }
}
