package io.github.mike10004.seleniumcapture.testbases;

import com.google.common.base.MoreObjects;
import com.google.common.base.Suppliers;
import io.github.mike10004.seleniumcapture.ImmutableHttpRequest;
import io.github.mike10004.seleniumcapture.ImmutableHttpResponse;
import io.github.mike10004.seleniumcapture.TrafficMonitor;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class HttpInteraction {

    public final ImmutableHttpRequest request;
    public final ImmutableHttpResponse response;
    private final Supplier<String> stringRepresentation;

    public HttpInteraction(ImmutableHttpRequest request, ImmutableHttpResponse response) {
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

    public static TrafficMonitor monitor(Consumer<? super HttpInteraction> interactionConsumer) {
        requireNonNull(interactionConsumer, "interactionConsumer");
        return new TrafficMonitor() {
            @Override
            public void responseReceived(ImmutableHttpRequest httpRequest, ImmutableHttpResponse httpResponse) {
                interactionConsumer.accept(new HttpInteraction(httpRequest, httpResponse));
            }

            @Override
            public String toString() {
                return "HttpInteractionMonitor{consumer=" + interactionConsumer + "}";
            }
        };
    }
}
