package com.github.mike10004.seleniumhelp;

import org.junit.Test;

import static org.junit.Assert.*;

public class WellDefinedSocketAddressTest {

    @Test
    public void testValidHostnamesAndValidPort() throws Exception {
        String[] testCases = {
                "1.2.3.4",
                "fe00::0",
                "ff00::0",
                "ff02::1",
                "ff02::2",
                "myproxy.net",
        };
        int port = 39484;
        for (String host : testCases) {
            new WellDefinedSocketAddress(host, port); // ok if no exception
        }
    }

    @Test
    public void testValidHostnameAndInvalidPorts() throws Exception {
        int[] testCases = {
                -1,
                0,
                65535 + 100,
                Integer.MAX_VALUE,
        };
        String host = "10.0.1.2";
        for (int port : testCases) {
            try {
                new WellDefinedSocketAddress(host, port);
                fail("expect illegalargument on port " + port);
            } catch (IllegalArgumentException ignore) {
            }
        }
    }

}