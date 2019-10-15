package com.github.mike10004.seleniumhelp;

import com.google.common.base.Strings;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class ProxySpecificationBase implements ProxySpecification {

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

    @Override
    @Nullable
    public org.openqa.selenium.Proxy createSeleniumProxy() {
        @Nullable FullSocketAddress hostAndPort = getSocketAddress();
        if (hostAndPort == null) {
            return null;
        }
        org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();
        proxy.setProxyType(org.openqa.selenium.Proxy.ProxyType.MANUAL);
        String socketAddress = String.format("%s:%d", hostAndPort.getHost(), hostAndPort.getPort());
        @Nullable String userInfo = getUsername();
        if (isSocks()) {
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
