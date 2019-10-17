package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.mitm.CertificateAndKeySource;

import javax.annotation.Nullable;

/**
 * Interface of a value class that defines a configuration for a session of webdriving.
 */
public interface WebdrivingConfig {

    /**
     * Gets the proxy specifiation, or null if no proxy is to be used. If HTTP traffic is to
     * be captured, then this is the intercepting proxy through which the webdriven browser
     * is to be configured to send network requests.
     * @return  proxy specification
     */
    WebdrivingProxyDefinition getProxySpecification();

    /**
     * Gets the certificate and key source to be used when middlemanning HTTPS traffic.
     * @return the certificate and key source
     */
    @Nullable
    CertificateAndKeySource getCertificateAndKeySource();

    /**
     * Returns a config instance that does not affect the webdriving session.
     * No traffic will be captured.
     * @return a
     */
    static WebdrivingConfig inactive() {
        return WebdrivingConfigs.empty();
    }

    /**
     * @deprecated use {@link #inactive()} instead
     */
    @Deprecated
    static WebdrivingConfig empty() {
        return inactive();
    }
}
