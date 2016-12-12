package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableSet;
import io.netty.handler.codec.http.HttpHeaders;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

import java.util.Map;

public class AnonymizingFiltersSource extends HttpFiltersSourceAdapter {

    private AnonymizingFiltersSource() {}

    private static final AnonymizingFiltersSource instance = new AnonymizingFiltersSource();

    public static HttpFiltersSource getInstance() {
        return instance;
    }

    private static final ImmutableSet<String> headersToRemove = ImmutableSet.of(
            HttpHeaders.Names.VIA,
            com.google.common.net.HttpHeaders.X_FORWARDED_FOR,
            com.google.common.net.HttpHeaders.X_FORWARDED_HOST,
            com.google.common.net.HttpHeaders.X_FORWARDED_PORT,
            com.google.common.net.HttpHeaders.X_FORWARDED_PROTO,
            com.google.common.net.HttpHeaders.X_USER_IP,
            "User-IP",
            "Client-IP",
            "X-Client-IP");

    private static final ImmutableSet<? extends Map.Entry<String, ?>> headersToReplace = ImmutableSet.of();
    private static final ImmutableSet<? extends Map.Entry<String, ?>> headersToAdd = ImmutableSet.of();

    @Override
    public HttpFilters filterRequest(io.netty.handler.codec.http.HttpRequest originalRequest) {
        return new HeaderModifyingRequestFilters(originalRequest, headersToRemove, headersToReplace, headersToAdd);
    }
}
