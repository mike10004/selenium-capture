package com.github.mike10004.seleniumhelp;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class RejectingFiltersSource extends HttpFiltersSourceAdapter {

    private final AtomicInteger requestCounter = new AtomicInteger(0);

    @Override
    public HttpFilters filterRequest(HttpRequest originalRequest) {
        requestCounter.incrementAndGet();
        HttpFilters rejectingFilters = new HttpFiltersAdapter(originalRequest) {
            @Override
            public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                if (isRejectTarget(originalRequest, httpObject)) {
                    return createResponse(originalRequest, httpObject);
                }
                return null;
            }
        };
        return rejectingFilters;
    }

    @Override
    public final HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        return super.filterRequest(originalRequest, ctx);
    }

    protected abstract boolean isRejectTarget(HttpRequest originalRequest, HttpObject httpObject);

    protected DefaultFullHttpResponse createResponse(HttpRequest originalRequest, HttpObject httpObject) {
        Charset charset = StandardCharsets.UTF_8;
        ByteBuf content = Unpooled.wrappedBuffer(constructRejectionText(originalRequest, httpObject).getBytes(charset));
        DefaultFullHttpResponse rejection = new DefaultFullHttpResponse(originalRequest.getProtocolVersion(),
                HttpResponseStatus.INTERNAL_SERVER_ERROR, content);
        rejection.headers().set(HttpHeaders.CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.withCharset(charset));
        return rejection;
    }

    public int getRequestCount() {
        return requestCounter.get();
    }

    protected String constructRejectionText(HttpRequest originalRequest, HttpObject httpObject) {
        return "error";
    }
}
