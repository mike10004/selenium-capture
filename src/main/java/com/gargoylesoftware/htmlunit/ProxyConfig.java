/*
 * Copyright (c) 2002-2016 Gargoyle Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gargoylesoftware.htmlunit;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Verbatim copy of HTMLUnit's class of the same name.
 * Class which centralizes proxy configuration, in an effort to reduce clutter in
 * the {@code WebClient} class. One instance of this class exists for
 * each <tt>WebClient</tt> instance.
 *
 * @author Daniel Gredler
 * @deprecated included in this library to facilitate transitions from using HTMLUnit to Selenium for automation,
 * but your code should quit using this class eventually. It is not deprecated in the real HTMLUnit library
 */
@Deprecated
public class ProxyConfig implements Serializable {

    private String proxyHost_;
    private int proxyPort_;
    private boolean isSocksProxy_;
    private final Map<String, Pattern> proxyBypassHosts_ = new HashMap<>();
    private String proxyAutoConfigUrl_;
    private String proxyAutoConfigContent_;

    /**
     * Creates a new instance.
     */
    public ProxyConfig() {
        this(null, 0);
    }

    /**
     * Creates a new instance.
     * @param proxyHost the proxy host
     * @param proxyPort the proxy port
     */
    public ProxyConfig(final String proxyHost, final int proxyPort) {
        this(proxyHost, proxyPort, false);
    }

    /**
     * Creates a new instance.
     * @param proxyHost the proxy host
     * @param proxyPort the proxy port
     * @param isSocks whether SOCKS proxy or not
     */
    public ProxyConfig(final String proxyHost, final int proxyPort, final boolean isSocks) {
        proxyHost_ = proxyHost;
        proxyPort_ = proxyPort;
        isSocksProxy_ = isSocks;
    }

    /**
     * Returns the proxy host used to perform HTTP requests.
     * @return the proxy host used to perform HTTP requests
     */
    public String getProxyHost() {
        return proxyHost_;
    }

    /**
     * Sets the proxy host used to perform HTTP requests.
     * @param proxyHost the proxy host used to perform HTTP requests
     */
    public void setProxyHost(final String proxyHost) {
        proxyHost_ = proxyHost;
    }

    /**
     * Returns the proxy port used to perform HTTP requests.
     * @return the proxy port used to perform HTTP requests
     */
    public int getProxyPort() {
        return proxyPort_;
    }

    /**
     * Sets the proxy port used to perform HTTP requests.
     * @param proxyPort the proxy port used to perform HTTP requests
     */
    public void setProxyPort(final int proxyPort) {
        proxyPort_ = proxyPort;
    }

    /**
     * Returns whether SOCKS proxy or not.
     * @return whether SOCKS proxy or not
     */
    public boolean isSocksProxy() {
        return isSocksProxy_;
    }

    /**
     * Sets whether SOCKS proxy or not.
     * @param isSocksProxy whether SOCKS proxy or not
     */
    public void setSocksProxy(final boolean isSocksProxy) {
        isSocksProxy_ = isSocksProxy;
    }

    /**
     * Any hosts matched by the specified regular expression pattern will bypass the configured proxy.
     * @param pattern a regular expression pattern that matches the hostnames of the hosts which should
     *                bypass the configured proxy.
     * @see Pattern
     */
    public void addHostsToProxyBypass(final String pattern) {
        proxyBypassHosts_.put(pattern, Pattern.compile(pattern));
    }

    /**
     * Any hosts matched by the specified regular expression pattern will no longer bypass the configured proxy.
     * @param pattern the previously added regular expression pattern
     * @see Pattern
     */
    public void removeHostsFromProxyBypass(final String pattern) {
        proxyBypassHosts_.remove(pattern);
    }

    /**
     * Returns {@code true} if the host with the specified hostname should be accessed bypassing the
     * configured proxy.
     * @param hostname the name of the host to check
     * @return {@code true} if the host with the specified hostname should be accessed bypassing the
     * configured proxy, {@code false} otherwise.
     */
    protected boolean shouldBypassProxy(final String hostname) {
        boolean bypass = false;
        for (final Pattern p : proxyBypassHosts_.values()) {
            if (p.matcher(hostname).find()) {
                bypass = true;
                break;
            }
        }
        return bypass;
    }

    /**
     * Returns the proxy auto-config URL.
     * @return the proxy auto-config URL
     */
    public String getProxyAutoConfigUrl() {
        return proxyAutoConfigUrl_;
    }

    /**
     * Sets the proxy auto-config URL.
     * @param proxyAutoConfigUrl the proxy auto-config URL
     */
    public void setProxyAutoConfigUrl(final String proxyAutoConfigUrl) {
        proxyAutoConfigUrl_ = proxyAutoConfigUrl;
        setProxyAutoConfigContent(null);
    }

    /**
     * Returns the proxy auto-config content.
     * @return the proxy auto-config content
     */
    protected String getProxyAutoConfigContent() {
        return proxyAutoConfigContent_;
    }

    /**
     * Sets the proxy auto-config content.
     * @param proxyAutoConfigContent the proxy auto-config content
     */
    protected void setProxyAutoConfigContent(final String proxyAutoConfigContent) {
        proxyAutoConfigContent_ = proxyAutoConfigContent;
    }
}