package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.jcraft.jzlib.GZIPInputStream;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import net.lightbody.bmp.filters.ServerResponseCaptureFilter;
import net.lightbody.bmp.util.BrowserMobHttpUtil;
import org.apache.commons.lang3.StringUtils;
import org.brotli.dec.BrotliInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Brotli-aware server response capture filter. Copied from {@link net.lightbody.bmp.filters.ServerResponseCaptureFilter}
 * but adds support for {@code br} encoding to the existing {@code gz} support.
 */
@SuppressWarnings({"unused", "StatementWithEmptyBody", "BooleanParameter", "SimplifiableIfStatement"})
public class BrAwareServerResponseCaptureFilter  extends ServerResponseCaptureFilter {

    public static final String HEADER_VALUE_BROTLI_ENCODING = "br";

    private static final Logger log = LoggerFactory.getLogger(BrAwareServerResponseCaptureFilter.class);

    /**
     * Populated by serverToProxyResponse() when processing the HttpResponse object
     */
    private volatile HttpResponse httpResponse;

    /**
     * Populated by serverToProxyResponse() as it receives HttpContent responses. If the response is chunked, it will
     * be populated across multiple calls to proxyToServerResponse().
     */
    private final ByteArrayOutputStream rawResponseContents = new ByteArrayOutputStream();

    /**
     * Populated when processing the LastHttpContent. If the response is compressed and decompression is requested,
     * this contains the entire decompressed response. Otherwise it contains the raw response.
     */
    private volatile byte[] fullResponseContents;

    /**
     * Populated by serverToProxyResponse() when it processes the LastHttpContent object.
     */
    private volatile HttpHeaders trailingHeaders;

    /**
     * Set to true when processing the LastHttpContent if the server indicates there is a content encoding.
     */
    private volatile boolean responseCompressed;

    /**
     * Set to true when processing the LastHttpContent if decompression was requested and successful.
     */
    private volatile boolean decompressionSuccessful;

    /**
     * Content-encoding header values, added as they are encountered.
     */
    private final List<String> contentEncodings = new ArrayList<>(4);

    /**
     * User option indicating compressed content should be uncompressed.
     */
    private final boolean decompressEncodedContent;

    public BrAwareServerResponseCaptureFilter(HttpRequest originalRequest, boolean decompressEncodedContent) {
        super(originalRequest, decompressEncodedContent);
        this.decompressEncodedContent = decompressEncodedContent;
    }

    public BrAwareServerResponseCaptureFilter(HttpRequest originalRequest, ChannelHandlerContext ctx, boolean decompressEncodedContent) {
        super(originalRequest, ctx, decompressEncodedContent);
        this.decompressEncodedContent = decompressEncodedContent;
    }

    @Override
    public HttpObject serverToProxyResponse(HttpObject httpObject) {
        if (httpObject instanceof HttpResponse) {
            httpResponse = (HttpResponse) httpObject;
            captureContentEncoding(httpResponse);
        }

        if (httpObject instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) httpObject;

            storeResponseContent(httpContent);

            if (httpContent instanceof LastHttpContent) {
                LastHttpContent lastContent = (LastHttpContent) httpContent;
                captureTrailingHeaders(lastContent);

                captureFullResponseContents();
            }
        }

        return httpObject;
    }

    private boolean isContentEncodingSpecified() {
        return !contentEncodings.isEmpty();
    }

    @Override
    protected void captureFullResponseContents() {
        // start by setting fullResponseContent to the raw, (possibly) compressed byte stream. replace it
        // with the decompressed bytes if decompression is successful.
        fullResponseContents = getRawResponseContents();

        // if the content is compressed, we need to decompress it. but don't use
        // the netty HttpContentCompressor/Decompressor in the pipeline because we don't actually want it to
        // change the message sent to the client
        if (isContentEncodingSpecified()) {
            responseCompressed = true;

            if (decompressEncodedContent) {
                decompressContents();
            }  else {
                // will not decompress response
            }
        } else {
            // no compression
            responseCompressed = false;
        }
    }

    protected interface DecompressionFilter {
        InputStream openStream(InputStream compressedDataStream) throws IOException;

        DecompressionFilter IDENTITY = input -> input;

        static DecompressionFilter concatenate(Iterable<DecompressionFilter> filters) {
            return new DecompressionFilter() {
                @Override
                public InputStream openStream(InputStream compressedDataStream) throws IOException {
                    for (DecompressionFilter filter : filters) {
                        compressedDataStream = filter.openStream(compressedDataStream);
                    }
                    return compressedDataStream;
                }
            };
        }
    }

    protected byte[] decompressContents(byte[] compressedBytes, DecompressionFilter decompressor) throws IOException {
        byte[] decompressed;
        try (InputStream in = decompressor.openStream(new ByteArrayInputStream(compressedBytes))) {
            decompressed = ByteStreams.toByteArray(in);
        }
        return decompressed;
    }

    /**
     * Explodes a content-encoding value into one or more individual token values.
     * This means that {@code gzip,br} is broken into a list {@code [gzip, br]}.
     * @param value the content-encoding header value
     * @return one or more non-multiple tokens representing the original value
     */
    protected Stream<String> explodeContentEncoding(String value) {
        return Stream.of(value.split(",\\s*"));
    }

    static class UnsupportedContentEncodingException extends IOException {
        public UnsupportedContentEncodingException(String encoding) {
            super(StringUtils.abbreviate(encoding, 32));
        }
    }

    protected DecompressionFilter createDecompressor(String singleEncoding) throws UnsupportedContentEncodingException {
        switch (singleEncoding) {
            case HttpHeaders.Values.GZIP:
                return GZIPInputStream::new;
            case HEADER_VALUE_BROTLI_ENCODING:
                return BrotliInputStream::new;
            case "identity":
                return DecompressionFilter.IDENTITY;
            default:
                throw new UnsupportedContentEncodingException(singleEncoding);
        }
    }

    protected DecompressionFilter createDecompressor(List<String> contentEncodings) throws UnsupportedContentEncodingException {
        List<String> singleEncodings = contentEncodings.stream().flatMap(this::explodeContentEncoding).collect(Collectors.toList());
        List<DecompressionFilter> stages = new ArrayList<>(singleEncodings.size());
        for (String singleEncoding : singleEncodings) {
            stages.add(createDecompressor(singleEncoding));
        }
        return DecompressionFilter.concatenate(stages);
    }

    @Override
    protected void decompressContents() {
        ImmutableList<String> contentEncodings = ImmutableList.copyOf(this.contentEncodings);
        if (!contentEncodings.isEmpty()) {
            try {
                DecompressionFilter decompressor = createDecompressor(contentEncodings);
                fullResponseContents = decompressContents(getRawResponseContents(), decompressor);
                decompressionSuccessful = true;
            } catch (RuntimeException | IOException e) {
                log.warn("Failed to decompress response with encodings " + contentEncodings + " when decoding request from " + originalRequest.getUri(), e);
            }
        } else {
            log.warn("Cannot decode unsupported content encoding type {}", contentEncodings);
        }
    }

    @Override
    protected void captureContentEncoding(HttpResponse httpResponse) {
        String value = HttpHeaders.getHeader(httpResponse, HttpHeaders.Names.CONTENT_ENCODING);
        if (value != null) {
            contentEncodings.add(value);
        }
    }

    @Override
    protected void captureTrailingHeaders(LastHttpContent lastContent) {
        trailingHeaders = lastContent.trailingHeaders();

        // technically, the Content-Encoding header can be in a trailing header, although this is excruciatingly uncommon
        if (trailingHeaders != null) {
            String trailingContentEncoding = trailingHeaders.get(HttpHeaders.Names.CONTENT_ENCODING);
            if (trailingContentEncoding != null) {
                contentEncodings.add(trailingContentEncoding);
            }
        }

    }

    @Override
    protected void storeResponseContent(HttpContent httpContent) {
        ByteBuf bufferedContent = httpContent.content();
        byte[] content = BrowserMobHttpUtil.extractReadableBytes(bufferedContent);

        try {
            rawResponseContents.write(content);
        } catch (IOException e) {
            // can't happen
        }
    }

    @Override
    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    /**
     * Returns the contents of the entire response. If the contents were compressed, <code>decompressEncodedContent</code> is true, and
     * decompression was successful, this method returns the decompressed contents.
     *
     * @return entire response contents, decompressed if possible
     */
    @Override
    public byte[] getFullResponseContents() {
        return fullResponseContents;
    }

    /**
     * Returns the raw contents of the entire response, without decompression.
     *
     * @return entire response contents, without decompression
     */
    @Override
    public byte[] getRawResponseContents() {
        return rawResponseContents.toByteArray();
    }

    @Override
    public HttpHeaders getTrailingHeaders() {
        return trailingHeaders;
    }

    @Override
    public boolean isResponseCompressed() {
        return responseCompressed;
    }

    /**
     * @return true if decompression is both enabled and successful
     */
    @Override
    public boolean isDecompressionSuccessful() {
        if (!decompressEncodedContent) {
            return false;
        }

        return decompressionSuccessful;
    }

    @Override
    public String getContentEncoding() {
        return mergeEncodings(contentEncodings);
    }

    protected String mergeEncodings(Iterable<String> contentEncodings) {
        return String.join(", ", contentEncodings);
    }
}
