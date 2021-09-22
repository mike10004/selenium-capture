package io.github.mike10004.seleniumcapture;

import com.browserup.harreader.model.HarHeader;
import com.browserup.harreader.model.HarPostDataParam;
import com.browserup.harreader.model.HarQueryParam;
import com.browserup.harreader.model.HarResponse;
import com.browserup.harreader.model.HttpMethod;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HostAndPort;
import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.client.ClientUtil;

import java.net.InetAddress;

import static java.util.Objects.requireNonNull;

/**
 * Static utility methods relating to the Browserup Proxy library.
 */
public class BrowserUps {

    private BrowserUps() {
    }

    /**
     * @deprecated  use {@link #getInstanceSocketAddress(BrowserUpProxy)}
     */
    @Deprecated
    public static HostAndPort getConnectableSocketAddress(BrowserUpProxy proxy) {
        FullSocketAddress socketAddress = resolveSocketAddress(proxy);
        return HostAndPort.fromParts(socketAddress.getHost(), socketAddress.getPort());
    }

    /**
     * Gets the socket address for the given proxy instance.
     * @param browserMobProxy the proxy instance
     * @return the socket address
     * @see ClientUtil#createSeleniumProxy(BrowserUpProxy)
     */
    public static FullSocketAddress resolveSocketAddress(BrowserUpProxy browserMobProxy) {
        InetAddress address = ClientUtil.getConnectableAddress();
        return FullSocketAddress.define(toLiteral(address), browserMobProxy.getPort());
    }

    private static String toLiteral(InetAddress address) {
        return requireNonNull(address, "address").getHostAddress();
    }

    static void setHarResponseError(HarResponse harResponse, String message) {
        harResponse.setAdditionalField("_errorMessage", message);
    }

    @VisibleForTesting
    static HttpMethod toHarHttpMethod(io.netty.handler.codec.http.HttpMethod method) {
        return HttpMethod.valueOf(method.name().toUpperCase());
    }

    static HarPostDataParam newHarPostDataParam(String name, String value) {
        HarPostDataParam p = new HarPostDataParam();
        p.setName(name);
        p.setValue(value);
        return p;
    }

    static HarHeader newHarHeader(String name, String value) {
        HarHeader h = new HarHeader();
        h.setName(name);
        h.setValue(value);
        return h;
    }

    static HarQueryParam newHarQueryParam(String name, String value) {
        HarQueryParam h = new HarQueryParam();
        h.setName(name);
        h.setValue(value);
        return h;
    }
}
