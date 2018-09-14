package com.github.mike10004.seleniumhelp;

import io.netty.handler.codec.http.HttpObject;
import org.littleshoot.proxy.HttpFiltersSource;
import org.openqa.selenium.WebDriver;

import java.lang.ref.WeakReference;

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

    /**
     * Gets teh max request buffer size.
     * @return max request buffer size in bytes
     * @see HttpFiltersSource#getMaximumRequestBufferSizeInBytes()
     */
    default int getMaximumRequestBufferSizeInBytes() {
        return 0;
    }

    /**
     * Gets the max response buffer size.
     * @return max response buffer size in bytes
     * @see HttpFiltersSource#getMaximumResponseBufferSizeInBytes()
     */
    default int getMaximumResponseBufferSizeInBytes() {
        return 0;
    }

    @SuppressWarnings("unused")
    default void sessionCreated(WeakReference<WebdrivingSession> session) {
        // no op
    }
}
