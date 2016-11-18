package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import org.littleshoot.proxy.HttpFiltersAdapter;

import java.util.Map;
import java.util.Map.Entry;

class HeaderModifyingRequestFilters extends HttpFiltersAdapter {

    private final ImmutableSet<String> headersToRemove;
    private final ImmutableList<? extends Entry<String, ?>> headersToReplace;
    private final ImmutableList<? extends Entry<String, ?>> headersToAdd;

    public HeaderModifyingRequestFilters(HttpRequest originalRequest, Iterable<String> headersToRemove, Iterable<? extends Entry<String, ?>> headersToReplace, Iterable<? extends Entry<String, ?>> headersToAdd) {
        super(originalRequest);
        this.headersToRemove = ImmutableSet.copyOf(headersToRemove);
        this.headersToReplace = ImmutableList.copyOf(headersToReplace);
        this.headersToAdd = ImmutableList.copyOf(headersToAdd);
    }

    @Override
    public io.netty.handler.codec.http.HttpResponse proxyToServerRequest(io.netty.handler.codec.http.HttpObject httpObject) {
        if (httpObject instanceof HttpRequest) {
            HttpRequest request =
                    ((HttpRequest) httpObject);
            io.netty.handler.codec.http.HttpHeaders headers = request.headers();
            customize(headers);
        }
        return null;
    }

    protected void customize(HttpHeaders headers) {
        for (String headerName : headersToRemove) {
            headers.remove(headerName);
        }
        for (Entry<String, ?> header : headersToReplace) {
            headers.set(header.getKey(), header.getValue());
        }
        for (Entry<String, ?> header : headersToAdd) {
            headers.add(header.getKey(), header.getValue());
        }
    }
}
