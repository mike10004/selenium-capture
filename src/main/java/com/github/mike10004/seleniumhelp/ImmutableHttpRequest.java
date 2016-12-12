package com.github.mike10004.seleniumhelp;

import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImmutableHttpRequest implements HttpRequest {

    private final HttpRequest inner;
    private final Supplier<HttpHeaders> headersSupplier;

    public ImmutableHttpRequest(HttpRequest inner) {
        this.inner = checkNotNull(inner);
        headersSupplier = ImmutableHttpHeaders.memoize(inner);
    }

    @Override
    public HttpRequest setMethod(HttpMethod method) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public HttpRequest setUri(String uri) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public HttpRequest setProtocolVersion(HttpVersion version) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public HttpHeaders headers() {
        return headersSupplier.get();
    }

    @Override
    public void setDecoderResult(DecoderResult result) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public HttpMethod getMethod() {
        return inner.getMethod();
    }

    @Override
    public String getUri() {
        return inner.getUri();
    }

    @Override
    public HttpVersion getProtocolVersion() {
        return inner.getProtocolVersion();
    }

    @Override
    public DecoderResult getDecoderResult() {
        return inner.getDecoderResult();
    }

    @Override
    public String toString() {
        return inner.toString();
    }
}
