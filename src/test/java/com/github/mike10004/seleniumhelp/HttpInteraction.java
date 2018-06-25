package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.base.Suppliers;

import java.util.function.Supplier;

class HttpInteraction {

    public final ImmutableHttpRequest request;
    public final ImmutableHttpResponse response;
    private final Supplier<String> stringRepresentation;

    HttpInteraction(ImmutableHttpRequest request, ImmutableHttpResponse response) {
        this.request = request;
        this.response = response;
        stringRepresentation = Suppliers.memoize(() -> {
            return MoreObjects.toStringHelper(HttpInteraction.class).add("request", request).add("response", response).toString();
        });
    }

    public ImmutableHttpRequest getRequest() {
        return request;
    }

    public ImmutableHttpResponse getResponse() {
        return response;
    }

    @Override
    public String toString() {
        return stringRepresentation.get();
    }
}
