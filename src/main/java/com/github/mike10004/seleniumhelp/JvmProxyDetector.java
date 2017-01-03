package com.github.mike10004.seleniumhelp;

import com.google.common.annotations.VisibleForTesting;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

public class JvmProxyDetector {

    public @Nullable InetSocketAddress detectJvmProxy() {
        return detectJvmProxy(System.getProperties());
    }

    /**
     * Detects JVM proxy settings.
     * @param sysprops the system properties
     * @return the socket address of the proxy
     * @throws IllegalStateException if the HTTP and HTTPS proxy settings are inconsistent
     * @throws NumberFormatException if the proxy ports are not integers
     */
    @VisibleForTesting
    protected @Nullable InetSocketAddress detectJvmProxy(Properties sysprops) throws IllegalStateException, NumberFormatException {
        String httpProxyHost = sysprops.getProperty("http.proxyHost");
        String httpProxyPort = sysprops.getProperty("http.proxyPort");
        String httpsProxyHost = sysprops.getProperty("http.proxyHost");
        String httpsProxyPort = sysprops.getProperty("http.proxyPort");
        if (!Objects.equals(httpProxyHost, httpsProxyHost)) {
            throw new IllegalStateException("system properties define conflicting values for http.proxyHost=" + httpProxyHost + " and httpsProxyHost=" + httpsProxyHost);
        }
        if (!Objects.equals(httpProxyPort, httpsProxyPort)) {
            throw new IllegalStateException("system properties define conflicting values for http.proxyPort=" + httpProxyPort + " and httpsProxyPort=" + httpsProxyPort);
        }
        if ((httpsProxyHost == null) != (httpsProxyPort == null)) {
            throw new IllegalStateException("nullness of https.proxyHost=" + httpsProxyHost + " and https.proxyPort=" + httpsProxyPort + " system properties must be consistent");
        }
        if (httpsProxyHost != null) {
            return new InetSocketAddress(httpsProxyHost, Integer.parseInt(httpsProxyPort));
        } else {
            return null;
        }
    }

    public Supplier<java.util.Optional<InetSocketAddress>> asOptionalSupplier() {
        return () -> java.util.Optional.ofNullable(detectJvmProxy());
    }

}
