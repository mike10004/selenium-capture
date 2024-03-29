/*
   Copyright Patrick Lightbody (net.lightbody.bmp.filters.HarCaptureFilter)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package io.github.mike10004.seleniumcapture;

import com.browserup.bup.exception.UnsupportedCharsetException;
import com.browserup.bup.filters.ClientRequestCaptureFilter;
import com.browserup.bup.filters.HttpsAwareFiltersAdapter;
import com.browserup.bup.filters.ResolvedHostnameCacheFilter;
import com.browserup.bup.filters.ServerResponseCaptureFilter;
import com.browserup.bup.filters.util.HarCaptureUtil;
import com.browserup.bup.util.BrowserUpHttpUtil;
import com.browserup.harreader.model.HarPostData;
import com.browserup.harreader.model.HarPostDataParam;
import com.browserup.harreader.model.HarRequest;
import com.browserup.harreader.model.HarResponse;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapMaker;
import com.google.common.io.BaseEncoding;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.littleshoot.proxy.impl.ProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of {@code HttpFilters} that sends a notification when an HTTP response
 * is received. This is still a pretty rough implementation, but it may suffice for
 * limited use cases at this stage. Don't expect the HTTP request/response objects
 * contained in the notification to be perfect representations.
 *
 * <p>Most of this code is copied from {@code net.lightbody.bmp.filters.HarCaptureFilter}.
 * The main difference is that the HAR filter keeps adding to the HAR object when
 * HTTP requests or responses are intercepted, but this implementation accumulates
 * (most of) the same data and packages it up in a notification when the request/response
 * interaction is completed. The interaction is considered completed when one of four methods
 * is invoked:
 * <ul>
 *     <li>{@link #serverToProxyResponse(HttpObject)} if everything goes normally,</li>
 *     <li>or one of the failure methods:
 *        <ul>
 *            <li>{@link #serverToProxyResponseTimedOut()}</li>
 *            <li>{@link #proxyToServerConnectionFailed()}</li>
 *            <li>{@link #proxyToServerResolutionFailed(String)}</li>
 *        </ul>
 *     </li>
 * </ul>
 */
public class TrafficMonitorFilter extends HttpsAwareFiltersAdapter {

    private static final Logger log = LoggerFactory.getLogger(TrafficMonitorFilter.class);

    private final TrafficMonitor trafficMonitor;
    private transient final Object notificationLock = new Object();
    private volatile boolean notifiedResponse;
    private final HarRequest harRequest = new HarRequest();
    private final HarResponse normalHarResponse = _createDefaultResponse();

    /**
     * The requestCaptureFilter captures all request content, including headers, trailing headers, and content. This filter
     * delegates to it when the clientToProxyRequest() callback is invoked. If this request does not need content capture, the
     * ClientRequestCaptureFilter filter will not be instantiated and will not capture content.
     */
    private final ClientRequestCaptureFilter requestCaptureFilter;

    /**
     * Like requestCaptureFilter above, HarCaptureFilter delegates to responseCaptureFilter to capture response contents. If content capture
     * is not required for this request, the filter will not be instantiated or invoked.
     */
    private final ServerResponseCaptureFilter responseCaptureFilter;

    /**
     * The "real" original request, as captured by the {@link #clientToProxyRequest(io.netty.handler.codec.http.HttpObject)} method.
     */
    private volatile HttpRequest capturedOriginalRequest;

    /**
     * True if this filter instance processed a {@link #proxyToServerResolutionSucceeded(String, java.net.InetSocketAddress)} call, indicating
     * that the hostname was resolved and populated in the HAR (if this is not a CONNECT).
     */
    private volatile boolean addressResolved = false;

    @SuppressWarnings("unused")
    private String serverIpAddress;

    /**
     * Create a new instance of the HarCaptureFilter that will capture request and response information. If no har is specified in the
     * constructor, this filter will do nothing.
     * <p>
     * Regardless of the CaptureTypes specified in <code>dataToCapture</code>, the HarCaptureFilter will always capture:
     * <ul>
     *     <li>Request and response sizes</li>
     *     <li>HTTP request and status lines</li>
     *     <li>Page timing information</li>
     * </ul>
     * @param originalRequest the original HttpRequest from the HttpFiltersSource factory
     * @param ctx channel handler context
     * @param trafficMonitor traffic monitor (subscriber to notifications from this filter)
     * @throws IllegalArgumentException if request method is {@code CONNECT}
     */
    public TrafficMonitorFilter(HttpRequest originalRequest, ChannelHandlerContext ctx, TrafficMonitor trafficMonitor) {
        super(originalRequest, ctx);
        if (ProxyUtils.isCONNECT(originalRequest)) {
            throw new IllegalArgumentException("Attempted traffic listener capture for HTTP CONNECT request");
        }
        requestCaptureFilter = new ClientRequestCaptureFilter(originalRequest);
        responseCaptureFilter = new ServerResponseCaptureFilter(originalRequest, true);
        this.trafficMonitor = checkNotNull(trafficMonitor);
    }

    private void sendResponseNotification(HarResponse harResponse) {
        synchronized (notificationLock) {
            if (notifiedResponse) {
                log.warn("already sent response notification; this will be suppressed: {}", harResponse);
                return;
            }
            ImmutableHttpRequest frozenRequest = HarInteractions.freeze(harRequest);
            trafficMonitor.responseReceived(frozenRequest, HarInteractions.freeze(harResponse));
            notifiedResponse = true;
        }
    }

    private static HarResponse _createDefaultResponse() {
        HarResponse defaultHarResponse = HarCaptureUtil.createHarResponseForFailure();
        defaultHarResponse.setAdditionalField("_error", HarCaptureUtil.getNoResponseReceivedErrorMessage());
        return defaultHarResponse;
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        // if a ServerResponseCaptureFilter is configured, delegate to it to collect the client request. if it is not
        // configured, we still need to capture basic information (timings, possibly client headers, etc.), just not content.
        requestCaptureFilter.clientToProxyRequest(httpObject);
        if (httpObject instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) httpObject;
            this.capturedOriginalRequest = httpRequest;
            // associate this request's HarRequest object with the har entry
            populateHarRequestFromHttpRequest(httpRequest, harRequest);
            captureQueryParameters(httpRequest, harRequest);
            captureRequestHeaders(httpRequest, harRequest);
        }

        if (httpObject instanceof LastHttpContent) {
            LastHttpContent lastHttpContent = (LastHttpContent) httpObject;
            captureTrailingHeaders(lastHttpContent, harRequest);
            captureRequestContent(requestCaptureFilter.getHttpRequest(), requestCaptureFilter.getFullRequestContents(), harRequest);
        }
        return null;
    }

    @Override
    public HttpObject proxyToClientResponse(HttpObject httpObject) {
        accumulateResponse(httpObject);
        return super.proxyToClientResponse(httpObject);
    }

    private final Set<HttpObject> responseObjectsAccumulated = createResponseObjectsSet();
    private transient final Object responseObjectsLock = new Object();

    /**
     * Creates a set that keeps track of which response object have already been seen and accumulated.
     * We avoid creating a hash map because the object hash may change over its life (because they are mutable).
     * We use weak keys just to be safe, even though this filter object should be garbage-collected after the
     * response is sent.
     * @return a set appropriate for tracking response objects
     */
    private static Set<HttpObject> createResponseObjectsSet() {
        Map<HttpObject, Boolean> map = new MapMaker().weakKeys().makeMap();
        return Collections.newSetFromMap(map);
    }

    static String getSimpleClassName(@Nullable Object object) {
        if (object == null) {
            return "null";
        }
        Class<?> theClass = object.getClass();
        String simpleName = theClass.getSimpleName();
        int iterations = 0;
        while (theClass != null && (simpleName = theClass.getSimpleName()).isEmpty()) {
            simpleName = theClass.getSimpleName();
            theClass = theClass.getSuperclass();
            iterations++;
        }
        simpleName = simpleName + com.google.common.base.Strings.repeat("$", iterations);
        return simpleName;
    }

    private void accumulateResponse(HttpObject httpObject) {
        synchronized (responseObjectsLock) {
            if (responseObjectsAccumulated.contains(httpObject)) {
                /*
                 * TODO figure out whether we should ignore all but the last instead of all but the first appearance of the response object
                 * Currently the response object is only be captured by the accumulator the
                 * first time the object is passed to this method. It's possible that what
                 * we want is for the object to be captured the **last** time it is passed
                 * to this method, to make sure we capture it final state. If we observe
                 * data not being captured, we should come back to this and re-evaluate.
                 */
                return;
            }
            responseObjectsAccumulated.add(httpObject);
            // if a ServerResponseCaptureFilter is configured, delegate to it to collect the server's response. if it is not
            // configured, we still need to capture basic information (timings, HTTP status, etc.), just not content.
            responseCaptureFilter.serverToProxyResponse(httpObject);
            if (httpObject instanceof HttpResponse) {
                HttpResponse httpResponse = (HttpResponse) httpObject;
                captureResponse(httpResponse, normalHarResponse);
            }
            if (httpObject instanceof LastHttpContent) {
                captureResponseContent(responseCaptureFilter.getHttpResponse(), responseCaptureFilter.getFullResponseContents(), normalHarResponse);
                sendResponseNotification(normalHarResponse);
            }


        }
    }

    @Override
    public HttpObject serverToProxyResponse(HttpObject httpObject) {
        accumulateResponse(httpObject);
        return super.serverToProxyResponse(httpObject);
    }

    /**
     * Populates a HarRequest object using the method, url, and HTTP version of the specified request.
     * @param httpRequest HTTP request on which the HarRequest will be based
     */
    private void populateHarRequestFromHttpRequest(HttpRequest httpRequest, HarRequest harRequest) {
        harRequest.setMethod(BrowserUps.toHarHttpMethod(httpRequest.method()));
        harRequest.setUrl(getFullUrl(httpRequest));
        harRequest.setHttpVersion(httpRequest.protocolVersion().text());
        // the HAR spec defines the request.url field as:
        //     url [string] - Absolute URL of the request (fragments are not included).
        // the URI on the httpRequest may only identify the path of the resource, so find the full URL.
        // the full URL consists of the scheme + host + port (if non-standard) + path + query params + fragment.
    }

    protected void captureQueryParameters(HttpRequest httpRequest, HarRequest harRequest) {
        // capture query parameters. it is safe to assume the query string is UTF-8, since it "should" be in US-ASCII (a subset of UTF-8),
        // but sometimes does include UTF-8 characters.
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.uri(), StandardCharsets.UTF_8);

        try {
            for (Map.Entry<String, List<String>> entry : queryStringDecoder.parameters().entrySet()) {
                for (String value : entry.getValue()) {
                    harRequest.getQueryString().add(BrowserUps.newHarQueryParam(entry.getKey(), value));
                }
            }
        } catch (IllegalArgumentException e) {
            // QueryStringDecoder will throw an IllegalArgumentException if it cannot interpret a query string. rather than cause the entire request to
            // fail by propagating the exception, simply skip the query parameter capture.
            log.info("Unable to decode query parameters on URI: " + httpRequest.uri(), e);
        }
    }

    protected void captureRequestHeaders(HttpRequest httpRequest, HarRequest harRequest) {
        HttpHeaders headers = httpRequest.headers();
        captureHeaders(headers, harRequest);
    }

    protected void captureTrailingHeaders(LastHttpContent lastHttpContent, HarRequest harRequest) {
        HttpHeaders headers = lastHttpContent.trailingHeaders();
        captureHeaders(headers, harRequest);
    }

    protected void captureHeaders(HttpHeaders headers, HarRequest harRequest) {
        for (Map.Entry<String, String> header : headers.entries()) {
            harRequest.getHeaders().add(BrowserUps.newHarHeader(header.getKey(), header.getValue()));
        }
    }

    protected void captureRequestContent(HttpRequest httpRequest, byte[] fullMessage, HarRequest harRequest) {
        if (fullMessage.length == 0) {
            return;
        }

        String contentType = httpRequest.headers().get(HttpHeaderNames.CONTENT_TYPE);
        if (contentType == null) {
            log.warn("No content type specified in request to {}. Content will be treated as {}", httpRequest.uri(), BrowserUpHttpUtil.UNKNOWN_CONTENT_TYPE);
            contentType = BrowserUpHttpUtil.UNKNOWN_CONTENT_TYPE;
        }

        HarPostData postData = new HarPostData();
        harRequest.setPostData(postData);

        postData.setMimeType(contentType);

        final boolean urlEncoded = contentType.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString());

        Charset charset;
        try {
            charset = BrowserUpHttpUtil.readCharsetInContentTypeHeader(contentType);
        } catch (UnsupportedCharsetException e) {
            log.warn("Found unsupported character set in Content-Type header '{}' in HTTP request to {}. Content will not be captured in HAR.", contentType, httpRequest.uri(), e);
            return;
        }

        if (charset == null) {
            // no charset specified, so use the default -- but log a message since this might not encode the data correctly
            charset = BrowserUpHttpUtil.DEFAULT_HTTP_CHARSET;
            log.debug("No charset specified; using charset {} to decode contents to {}", charset, httpRequest.uri());
        }

        if (urlEncoded) {
            String textContents = BrowserUpHttpUtil.getContentAsString(fullMessage, charset);

            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(textContents, charset, false);

            ImmutableList.Builder<HarPostDataParam> paramBuilder = ImmutableList.builder();

            for (Map.Entry<String, List<String>> entry : queryStringDecoder.parameters().entrySet()) {
                for (String value : entry.getValue()) {
                    paramBuilder.add(BrowserUps.newHarPostDataParam(entry.getKey(), value));
                }
            }

            harRequest.getPostData().setParams(paramBuilder.build());
        } else {
            //TODO: implement capture of files and multipart form data

            // not URL encoded, so let's grab the body of the POST and capture that
            String postBody = BrowserUpHttpUtil.getContentAsString(fullMessage, charset);
            harRequest.getPostData().setText(postBody);
        }
    }

    protected void captureResponseContent(HttpResponse httpResponse, byte[] fullMessage, HarResponse harResponse) {
        // force binary if the content encoding is not supported
        boolean forceBinary = false;

        String contentType = httpResponse.headers().get(HttpHeaderNames.CONTENT_TYPE);
        if (contentType == null) {
            log.warn("No content type specified in response from {}. Content will be treated as {}", originalRequest.uri(), BrowserUpHttpUtil.UNKNOWN_CONTENT_TYPE);
            contentType = BrowserUpHttpUtil.UNKNOWN_CONTENT_TYPE;
        }

        if (responseCaptureFilter.isResponseCompressed() && !responseCaptureFilter.isDecompressionSuccessful()) {
            log.warn("Unable to decompress content with encoding: {}. Contents will be encoded as base64 binary data.", responseCaptureFilter.getContentEncoding());
            forceBinary = true;
        }

        Charset charset;
        try {
            charset = BrowserUpHttpUtil.readCharsetInContentTypeHeader(contentType);
        } catch (UnsupportedCharsetException e) {
            log.warn("Found unsupported character set in Content-Type header '{}' in HTTP response from {}. Content will not be captured in HAR.", contentType, originalRequest.uri(), e);
            return;
        }

        if (charset == null) {
            // no charset specified, so use the default -- but log a message since this might not encode the data correctly
            charset = BrowserUpHttpUtil.DEFAULT_HTTP_CHARSET;
            log.debug("No charset specified; using charset {} to decode contents from {}", charset, originalRequest.uri());
        }

        if (!forceBinary && BrowserUpHttpUtil.hasTextualContent(contentType)) {
            String text = BrowserUpHttpUtil.getContentAsString(fullMessage, charset);
            harResponse.getContent().setText(text);
        } else {
            harResponse.getContent().setText(BaseEncoding.base64().encode(fullMessage));
            harResponse.getContent().setEncoding("base64");
        }

        harResponse.getContent().setSize(Long.valueOf(fullMessage.length));
    }

    protected void captureResponse(HttpResponse httpResponse, HarResponse harResponse) {
        harResponse.setStatus(httpResponse.status().code());
        harResponse.setStatusText(httpResponse.status().reasonPhrase());
        harResponse.setHttpVersion(httpResponse.protocolVersion().text());
        captureResponseHeaderSize(httpResponse, harResponse);
        captureResponseMimeType(httpResponse, harResponse);
        captureResponseHeaders(httpResponse, harResponse);
        if (BrowserUpHttpUtil.isRedirect(httpResponse)) {
            captureRedirectUrl(httpResponse, harResponse);
        }
    }

    protected void captureResponseMimeType(HttpResponse httpResponse, HarResponse harResponse) {
        String contentType = httpResponse.headers().get(HttpHeaderNames.CONTENT_TYPE);
        // don't set the mimeType to null, since mimeType is a required field
        if (contentType != null) {
            harResponse.getContent().setMimeType(contentType);
        }
    }

    protected void captureResponseHeaderSize(HttpResponse httpResponse, HarResponse harResponse) {
        String statusLine = httpResponse.protocolVersion().toString() + ' ' + httpResponse.status().toString();
        // +2 => CRLF after status line, +4 => header/data separation
        long responseHeadersSize = statusLine.length() + 6;
        HttpHeaders headers = httpResponse.headers();
        responseHeadersSize += BrowserUpHttpUtil.getHeaderSize(headers);

        harResponse.setHeadersSize(responseHeadersSize);
    }

    protected void captureResponseHeaders(HttpResponse httpResponse, HarResponse harResponse) {
        HttpHeaders headers = httpResponse.headers();
        for (Map.Entry<String, String> header : headers.entries()) {
            String name = header.getKey();
            String value = header.getValue();
            harResponse.getHeaders().add(BrowserUps.newHarHeader(name, value));
        }
    }

    protected void captureRedirectUrl(HttpResponse httpResponse, HarResponse harResponse) {
        String locationHeaderValue = httpResponse.headers().get(HttpHeaderNames.LOCATION);
        if (locationHeaderValue != null) {
            harResponse.setRedirectURL(locationHeaderValue);
        }
    }

    /**
     * Populates the serverIpAddress field of the harEntry using the internal hostname to IP address cache.
     *
     * @param httpRequest HTTP request to take the hostname from
     */
    protected void populateAddressFromCache(HttpRequest httpRequest) {
        String serverHost = getHost(httpRequest);

        if (serverHost != null && !serverHost.isEmpty()) {
            String resolvedAddress = ResolvedHostnameCacheFilter.getPreviouslyResolvedAddressForHost(serverHost);
            if (resolvedAddress != null) {
                serverIpAddress = (resolvedAddress);
            } else {
                // the resolvedAddress may be null if the ResolvedHostnameCacheFilter has expired the entry (which is unlikely),
                // or in the far more common case that the proxy is using a chained proxy to connect to connect to the
                // remote host. since the chained proxy handles IP address resolution, the IP address in the HAR must be blank.
                log.trace("Unable to find cached IP address for host: {}. IP address in HAR entry will be blank.", serverHost);
            }
        } else {
            log.warn("Unable to identify host from request uri: {}", httpRequest.uri());
        }
    }

    @Override
    public void proxyToServerResolutionSucceeded(String serverHostAndPort, InetSocketAddress resolvedRemoteAddress) {
        // the address *should* always be resolved at this point
        InetAddress resolvedAddress = resolvedRemoteAddress.getAddress();
        if (resolvedAddress != null) {
            addressResolved = true;
            serverIpAddress = (resolvedAddress.getHostAddress());
        }
    }

    @Override
    public void proxyToServerRequestSending() {
        // if the hostname was not resolved (and thus the IP address populated in the har) during this request, populate the IP address from the cache
        if (!addressResolved) {
            populateAddressFromCache(capturedOriginalRequest);
        }
    }

    @Override
    public void proxyToServerResolutionFailed(String hostAndPort) {
        HarResponse response = HarCaptureUtil.createHarResponseForFailure();
        BrowserUps.setHarResponseError(response, HarCaptureUtil.getResolutionFailedErrorMessage(hostAndPort));
        sendResponseNotification(response);
    }


    @Override
    public void proxyToServerConnectionFailed() {
        HarResponse response = HarCaptureUtil.createHarResponseForFailure();
        BrowserUps.setHarResponseError(response, HarCaptureUtil.getConnectionFailedErrorMessage());
        sendResponseNotification(response);
    }

    @Override
    public void serverToProxyResponseTimedOut() {
        // replace any existing HarResponse that was created if the server sent a partial response
        HarResponse response = HarCaptureUtil.createHarResponseForFailure();
        BrowserUps.setHarResponseError(response, HarCaptureUtil.getResponseTimedOutErrorMessage());
        sendResponseNotification(response);
    }

}
