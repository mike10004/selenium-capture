package com.github.mike10004.seleniumhelp;

import com.google.common.net.HostAndPort;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.client.ClientUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

/**
 * Static utility methods relating to the Browsermob Proxy library.
 */
public class BrowserMobs {

    private BrowserMobs() {
    }

    /**
     * Gets the socket address for the given proxy instance.
     * @param browserMobProxy the proxy instance
     * @return the socket address
     * @see ClientUtil#createSeleniumProxy(BrowserMobProxy)
     */
    public static HostAndPort getConnectableSocketAddress(BrowserMobProxy browserMobProxy) {
        InetAddress address = ClientUtil.getConnectableAddress();
        return HostAndPort.fromParts(toLiteral(address), browserMobProxy.getPort());
    }

    private static String toLiteral(InetAddress address) {
        return requireNonNull(address, "address").getHostAddress();
    }
}
