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
import net.lightbody.bmp.filters.HttpsAwareFiltersAdapter;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class RejectingFiltersSource extends HttpFiltersSourceAdapter {

    private final AtomicInteger requestCounter = new AtomicInteger(0);

    @Override
    public HttpFilters filterRequest(HttpRequest originalRequest) {
        return filterRequest(originalRequest, (ChannelHandlerContext) null);
    }

    @Override
    public final HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        requestCounter.incrementAndGet();
        HttpFilters rejectingFilters = new HttpsAwareFiltersAdapter(originalRequest, ctx) {
            @Override
            public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                String fullUrl = getOriginalUrl();
                if (isRejectTarget(originalRequest, fullUrl, httpObject)) {
                    return createResponse(originalRequest, fullUrl, httpObject);
                }
                return null;
            }
        };
        return rejectingFilters;
    }

    protected abstract boolean isRejectTarget(HttpRequest originalRequest, String fullUrl, HttpObject httpObject);

    protected DefaultFullHttpResponse createResponse(HttpRequest originalRequest, String fullUrl, HttpObject httpObject) {
        Charset charset = StandardCharsets.UTF_8;
        byte[] byteArray = constructRejectionText(originalRequest, fullUrl, httpObject).getBytes(charset);
        ByteBuf content = Unpooled.wrappedBuffer(byteArray);
        DefaultFullHttpResponse rejection = new DefaultFullHttpResponse(originalRequest.protocolVersion(),
                HttpResponseStatus.INTERNAL_SERVER_ERROR, content);
        rejection.headers().set(HttpHeaders.CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.withCharset(charset));
        rejection.headers().set(HttpHeaders.CONTENT_LENGTH, byteArray.length);
        return rejection;
    }

    public int getRequestCount() {
        return requestCounter.get();
    }

    protected String constructRejectionText(HttpRequest originalRequest, String fullUrl, HttpObject httpObject) {
        return "error";
    }
}
