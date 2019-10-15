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
    @Nullable
    ProxySpecification getProxySpecification();

    /**
     * Gets the certificate and key source to be used when middlemanning HTTPS traffic.
     * @return the certificate and key source
     */
    @Nullable
    CertificateAndKeySource getCertificateAndKeySource();

}
