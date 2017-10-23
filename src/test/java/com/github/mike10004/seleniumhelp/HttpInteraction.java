package com.github.mike10004.seleniumhelp;

class HttpInteraction {
    public final ImmutableHttpRequest request;
    public final ImmutableHttpResponse response;

    HttpInteraction(ImmutableHttpRequest request, ImmutableHttpResponse response) {
        this.request = request;
        this.response = response;
    }

    public ImmutableHttpRequest getRequest() {
        return request;
    }

    public ImmutableHttpResponse getResponse() {
        return response;
    }
}
