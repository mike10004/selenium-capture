package com.github.mike10004.seleniumhelp;

import com.google.common.net.HostAndPort;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.client.ClientUtil;

import java.net.InetAddress;

import static java.util.Objects.requireNonNull;

/**
 * Static utility methods relating to the Browsermob Proxy library.
 */
public class BrowserMobs {

    private BrowserMobs() {
    }

    /**
     * @deprecated  use {@link #getInstanceSocketAddress(BrowserMobProxy)}
     */
    @Deprecated
    public static HostAndPort getConnectableSocketAddress(BrowserMobProxy browserMobProxy) {
        FullSocketAddress socketAddress = resolveSocketAddress(browserMobProxy);
        return HostAndPort.fromParts(socketAddress.getHost(), socketAddress.getPort());
    }

    /**
     * Gets the socket address for the given proxy instance.
     * @param browserMobProxy the proxy instance
     * @return the socket address
     * @see ClientUtil#createSeleniumProxy(BrowserMobProxy)
     */
    public static FullSocketAddress resolveSocketAddress(BrowserMobProxy browserMobProxy) {
        InetAddress address = ClientUtil.getConnectableAddress();
        return FullSocketAddress.define(toLiteral(address), browserMobProxy.getPort());
    }

    private static String toLiteral(InetAddress address) {
        return requireNonNull(address, "address").getHostAddress();
    }
}
