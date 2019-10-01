package com.github.mike10004.seleniumhelp;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import org.brotli.dec.BrotliInputStream;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class BrAwareServerResponseCaptureFilterUnitTest {

    @Test
    public void decompressBrotliContents() throws Exception {
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, String.format("http://localhost:%d/blah", 12345));
        BrAwareServerResponseCaptureFilter filter = new BrAwareServerResponseCaptureFilter(request, true);
        byte[] brotliBytes = UnitTests.loadBrotliCompressedSample();
        byte[] decompressedBytes = filter.decompressContents(brotliBytes, BrotliInputStream::new);
        byte[] expected = UnitTests.loadBrotliUncompressedSample();
        assertArrayEquals("bytes", expected, decompressedBytes);
    }
}
