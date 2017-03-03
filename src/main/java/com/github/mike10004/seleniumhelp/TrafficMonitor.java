package com.github.mike10004.seleniumhelp;

import io.netty.handler.codec.http.HttpObject;

/**
 * Interface for classes that passively listen to HTTP requests and responses generated during
 * a collection session.
 * @see TrafficCollector#collect(TrafficGenerator, TrafficMonitor)
 */
public interface TrafficMonitor {

    /**
     * Callback invoked when a response from the remote server is received by the capturing proxy.
     * This method is invoked from {@link TrafficMonitorFilter#serverToProxyResponse(HttpObject)}
     * or one of that class's error methods.
     * @param httpResponse the HTTP response
     * @param httpRequest the HTTP request
     * @see TrafficMonitorFilter
     */
    void responseReceived(ImmutableHttpRequest httpRequest, ImmutableHttpResponse httpResponse);

}
