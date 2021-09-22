package io.github.mike10004.seleniumcapture;

import org.junit.Test;

import static org.junit.Assert.*;

public class BrAwareHarCaptureFilterTest {

    @Test
    public void toHarHttpMethod() {
        for (String methodName : new String[]{
                // copied from netty source code
                "OPTIONS", "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "TRACE", "CONNECT",
        }) {
            io.netty.handler.codec.http.HttpMethod nm = io.netty.handler.codec.http.HttpMethod.valueOf(methodName);
            io.netty.handler.codec.http.HttpMethod nmAgain = io.netty.handler.codec.http.HttpMethod.valueOf(methodName);
            assertSame("expect netty method object to be drawn from cache", nm, nmAgain);
            com.browserup.harreader.model.HttpMethod harHttpMethod = BrowserUps.toHarHttpMethod(nm);
            assertNotNull("har model http method", harHttpMethod);
        }
    }
}