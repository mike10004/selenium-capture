package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.client.ClientUtil;

import java.net.InetSocketAddress;

/**
 * Static utility methods relating to the Browsermob Proxy library.
 */
public class BrowserMobs {

    private BrowserMobs() {
    }

    /**
     *
     * @param browserMobProxy
     * @return
     * @see ClientUtil#createSeleniumProxy(BrowserMobProxy)
     */
    public static InetSocketAddress getConnectableSocketAddress(BrowserMobProxy browserMobProxy) {
        return new InetSocketAddress(ClientUtil.getConnectableAddress(), browserMobProxy.getPort());
    }

}
