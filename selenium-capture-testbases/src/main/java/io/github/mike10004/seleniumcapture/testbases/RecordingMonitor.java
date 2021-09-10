package io.github.mike10004.seleniumcapture.testbases;

import com.github.mike10004.seleniumhelp.ImmutableHttpRequest;
import com.github.mike10004.seleniumhelp.ImmutableHttpResponse;
import com.github.mike10004.seleniumhelp.TrafficMonitor;

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
