package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableMultimap;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ImmutableHttpMessageTest {

    @Test
    public void getHeaderValues() {
        ImmutableHttpMessage message = ImmutableHttpResponse.builder(200)
            .headers(ImmutableMultimap.of("Content-Type", "text/plain", "Foo", "Bar", "foo", "bar"))
            .build();
        assertEquals("exact case", "text/plain", message.getFirstHeaderValue("Content-Type"));
        assertEquals("diff case", "text/plain", message.getFirstHeaderValue("content-type"));
        assertEquals("all values", Arrays.asList("Bar", "bar"), message.getHeaderValues("foo").collect(Collectors.toList()));
        assertEquals("all values", Arrays.asList("Bar", "bar"), message.getHeaderValues("Foo").collect(Collectors.toList()));
    }
}