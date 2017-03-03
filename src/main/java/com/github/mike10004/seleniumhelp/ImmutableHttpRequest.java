package com.github.mike10004.seleniumhelp;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMultimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImmutableHttpRequest extends ImmutableHttpMessage {

    public final String method;
    public final URI url;
    private transient final Object paramsLock = new Object();
    private volatile ImmutableMultimap<String, String> queryParams;

    private ImmutableHttpRequest(Builder builder) {
        super(builder);
        this.method = checkNotNull(builder.method);
        this.url = checkNotNull(builder.url);
    }

    @Override
    public String toString() {
        return toStringHelper()
                .add("method", method)
                .add("url", StringUtils.abbreviate(url.toString(), 128))
                .toString();
    }

    public static Builder builder(URI requestUrl) {
        return new Builder(requestUrl);
    }

    public ImmutableMultimap<String, String> parseQueryParams() {
        return parseQueryParams(StandardCharsets.UTF_8);
    }

    public ImmutableMultimap<String, String> parseQueryParams(Charset charset) {
        synchronized (paramsLock) {
            if (queryParams == null) {
                List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(url, charset.name());
                ImmutableMultimap.Builder<String, String> b = ImmutableMultimap.builder();
                for (NameValuePair nameValuePair : nameValuePairs) {
                    b.put(Strings.nullToEmpty(nameValuePair.getName()), Strings.nullToEmpty(nameValuePair.getValue()));
                }
                queryParams = b.build();
            }
            return queryParams;
        }
    }

    @SuppressWarnings("unused")
    public static final class Builder extends MessageBuilder<Builder> {

        private String method = "GET";
        private final URI url;

        private Builder(URI url) {
            this.url = checkNotNull(url);
        }

        public Builder method(String method) {
            this.method = io.netty.handler.codec.http.HttpMethod.valueOf(method).name();
            return this;
        }

        public ImmutableHttpRequest build() {
            return new ImmutableHttpRequest(this);
        }
    }
}
