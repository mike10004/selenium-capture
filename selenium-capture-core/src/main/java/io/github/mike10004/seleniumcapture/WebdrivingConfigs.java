package io.github.mike10004.seleniumcapture;

import com.browserup.bup.mitm.CertificateAndKeySource;
import org.openqa.selenium.Proxy;

import javax.annotation.Nullable;

class WebdrivingConfigs {

    private WebdrivingConfigs() {}

    private static class NoWebdrivingProxyDefinition implements WebdrivingProxyDefinition {

        public NoWebdrivingProxyDefinition( ){}

        @Nullable
        @Override
        public Proxy createWebdrivingProxy() {
            return new org.openqa.selenium.Proxy().setProxyType(org.openqa.selenium.Proxy.ProxyType.DIRECT);
        }

        @Override
        public String toString() {
            return "WebdrivingProxyDefinition{DIRECT}";
        }
    }

    static WebdrivingProxyDefinition noWebdrivingProxyDefinition() {
        return new NoWebdrivingProxyDefinition();
    }

    private static final WebdrivingConfig EMPTY = new WebdrivingConfig() {


        @Override
        public WebdrivingProxyDefinition getProxySpecification() {
            return noWebdrivingProxyDefinition();
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
