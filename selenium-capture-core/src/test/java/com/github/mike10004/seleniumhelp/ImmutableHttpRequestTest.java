package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableMultimap;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

public class ImmutableHttpRequestTest {

    @Test
    public void parseQueryParams() throws Exception {
        URI uri = URI.create("http://example.com/hello?foo=bar");
        ImmutableHttpRequest request = ImmutableHttpRequest.builder(uri).build();
        assertEquals(ImmutableMultimap.of("foo", "bar"), request.parseQueryParams());
    }

}