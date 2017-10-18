package com.github.mike10004.seleniumhelp;

class HttpInteraction {
    public final ImmutableHttpRequest request;
    public final ImmutableHttpResponse response;

    HttpInteraction(ImmutableHttpRequest request, ImmutableHttpResponse response) {
        this.request = request;
        this.response = response;
    }
}
