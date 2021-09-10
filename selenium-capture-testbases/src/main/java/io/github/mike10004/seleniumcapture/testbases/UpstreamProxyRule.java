package io.github.mike10004.seleniumcapture.testbases;

import io.github.mike10004.seleniumcapture.UriProxySpecification;
import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import org.apache.http.client.utils.URIBuilder;
import org.junit.rules.ExternalResource;
import org.littleshoot.proxy.ChainedProxyType;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;

public class UpstreamProxyRule extends ExternalResource {
    public static final String SYSPROP_TEST_PROXY = "selenium-help.test.proxy.http";

    @Nullable
    private HostAndPort upstreamProxyHostAndPort = null;
    @Nullable
    private ChainedProxyType upstreamProxyType = null;

    @Override
    protected void before() throws Throwable {
        String proxyValue = System.getProperty(SYSPROP_TEST_PROXY);
        if (!Strings.isNullOrEmpty(proxyValue)) {
            if (proxyValue.matches("^\\w+://.*$")) {
                URI proxyUri = URI.create(proxyValue);
                upstreamProxyType = ChainedProxyType.valueOf(proxyUri.getScheme().toUpperCase());
                upstreamProxyHostAndPort = HostAndPort.fromParts(proxyUri.getHost(), proxyUri.getPort());
            } else {
                upstreamProxyHostAndPort = HostAndPort.fromString(proxyValue);
                upstreamProxyType = ChainedProxyType.HTTP;
            }
        } else {
            LoggerFactory.getLogger(CollectionTestBase.class).info("this test is much more valuable if you set system or maven property " + SYSPROP_TEST_PROXY + " to an available HTTP proxy that does not have the same external IP address as the JVM's network interface");
        }
    }

    @Nullable
    public URI getProxySpecificationUri() {
        if (upstreamProxyHostAndPort == null) {
            return null;
        }
        try {
            return new URIBuilder()
                    .setScheme(UriProxySpecification.toScheme(upstreamProxyType))
                    .setHost(upstreamProxyHostAndPort.getHost())
                    .setPort(upstreamProxyHostAndPort.getPort())
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public HostAndPort getUpstreamProxyHostAndPort() {
        return upstreamProxyHostAndPort;
    }

    @Nullable
    public ChainedProxyType getUpstreamProxyType() {
        return upstreamProxyType;
    }
}
