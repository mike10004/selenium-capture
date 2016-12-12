package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import org.littleshoot.proxy.HttpFiltersAdapter;

import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class HeaderModifyingRequestFilters extends HttpFiltersAdapter {

    private final ImmutableSet<String> headersToRemove;
    private final ImmutableList<? extends Entry<String, ?>> headersToReplace;
    private final ImmutableList<? extends Entry<String, ?>> headersToAdd;

    public HeaderModifyingRequestFilters(HttpRequest originalRequest, Iterable<String> headersToRemove, Iterable<? extends Entry<String, ?>> headersToReplace, Iterable<? extends Entry<String, ?>> headersToAdd) {
        super(originalRequest);
        this.headersToRemove = ImmutableSet.copyOf(headersToRemove);
        this.headersToReplace = ImmutableList.copyOf(headersToReplace);
        this.headersToAdd = ImmutableList.copyOf(headersToAdd);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Set<String> headersToRemove;
        private final List<Map.Entry<String, ?>> headersToReplace;
        private final List<Map.Entry<String, ?>> headersToAdd;

        private Builder() {
            headersToRemove = new LinkedHashSet<>();
            headersToReplace = new ArrayList<>();
            headersToAdd = new ArrayList<>();
        }

        public Builder add(String name, String value) {
            headersToAdd.add(new AbstractMap.SimpleImmutableEntry<>(checkNotNull(name, "name"), checkNotNull(value, "value")));
            return this;
        }

        public Builder remove(String name) {
            headersToRemove.add(checkNotNull(name, "name"));
            return this;
        }

        public Builder set(String name, String value) {
            headersToReplace.add(new AbstractMap.SimpleImmutableEntry<>(checkNotNull(name, "name"), checkNotNull(value, "value")));
            return this;
        }

        public HeaderModifyingRequestFilters build(HttpRequest originalRequest) {
            return new HeaderModifyingRequestFilters(originalRequest, headersToRemove, headersToReplace, headersToAdd);
        }
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
