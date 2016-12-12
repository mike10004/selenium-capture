package com.github.mike10004.seleniumhelp;

import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImmutableHttpResponse implements HttpResponse {

    private final HttpResponse inner;
    private final Supplier<HttpHeaders> headersSupplier;

    public ImmutableHttpResponse(HttpResponse inner) {
        this.inner = checkNotNull(inner);
        headersSupplier = ImmutableHttpHeaders.memoize(inner);
    }

    @Override
    public HttpResponseStatus getStatus() {
        return inner.getStatus();
    }

    @Override
    public HttpResponse setStatus(HttpResponseStatus status) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public HttpResponse setProtocolVersion(HttpVersion version) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public HttpHeaders headers() {
        return headersSupplier.get();
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
    public void setDecoderResult(DecoderResult result) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public String toString() {
        return inner.toString();
    }
}
