package io.github.mike10004.seleniumcapture.testbases;

import io.github.mike10004.seleniumcapture.ImmutableHttpRequest;
import io.github.mike10004.seleniumcapture.ImmutableHttpResponse;
import io.github.mike10004.seleniumcapture.TrafficMonitor;

import java.util.ArrayList;
import java.util.List;

class RecordingMonitor implements TrafficMonitor {

    public final List<HttpInteraction> interactions;

    @SuppressWarnings("unused")
    RecordingMonitor() {
        this(new ArrayList<>());
    }

    RecordingMonitor(List<HttpInteraction> interactions) {
        this.interactions = interactions;
    }

    @Override
    public void responseReceived(ImmutableHttpRequest httpRequest, ImmutableHttpResponse httpResponse) {
        interactions.add(new HttpInteraction(httpRequest, httpResponse));
    }
}
