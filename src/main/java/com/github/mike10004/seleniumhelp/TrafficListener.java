package com.github.mike10004.seleniumhelp;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 * Interface for classes that passively listen to HTTP requests and responses generated during
 * a collection session. Implementations must not modify the request or response within
 * their implemented methods.
 * @see TrafficCollector#collect(TrafficGenerator, TrafficListener)
 */
public interface TrafficListener {

    /**
     * Callback invoked when a response from the remote server is received by the capturing proxy.
     * This method is invoked from {@link TrafficCollector.ResponseNotificationFilter#serverToProxyResponse(HttpObject)}.
     * @param httpResponse the HTTP response
     */
    void responseReceived(HttpResponse httpResponse);

    /**
     * Callback invoked when a request is about to be sent from the capturing proxy to the remove server.
     * This method is invoked from {@link TrafficCollector.ResponseNotificationFilter#proxyToServerRequest(HttpObject)}.
     * @param httpRequest the HTTP request
     */
    void sendingRequest(HttpRequest httpRequest);

}
