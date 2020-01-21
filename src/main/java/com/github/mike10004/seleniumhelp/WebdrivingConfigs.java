package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.mitm.CertificateAndKeySource;

import javax.annotation.Nullable;

class WebdrivingConfigs {

    private WebdrivingConfigs() {}

    private static final WebdrivingConfig EMPTY = new WebdrivingConfig() {

        @Override
        public WebdrivingProxyDefinition getProxySpecification() {
            return NoProxySpecification.getInstance().asWebdrivingProxyDefinition();
        }

        @Nullable
        @Override
        public CertificateAndKeySource getCertificateAndKeySource() {
            return null;
        }

        @Override
        public String toString() {
            return "WebdrivingConfig{INACTIVE}";
        }
    };

    /**
     * Returns an immutable empty config instance.
     * @return an empty config instance.
     */
    public static WebdrivingConfig empty() {
        return EMPTY;
    }

    /**
     * @deprecated use {@link WebdrivingConfig#builder()}
     */
    @Deprecated
    public static WebdrivingConfig.Builder builder() {
        return WebdrivingConfig.builder();
    }
}
