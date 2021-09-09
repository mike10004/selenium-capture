package com.github.mike10004.seleniumhelp;

import org.junit.Test;

import static org.junit.Assert.*;

public class TrafficMonitorFilterTest {

    @Test
    public void getSimpleClassName() {
        assertEquals("null", "null", TrafficMonitorFilter.getSimpleClassName(null));
        assertEquals("anonymous subclass", "Something$", TrafficMonitorFilter.getSimpleClassName(new Something(){}));
        assertEquals("concrete class", "Something", TrafficMonitorFilter.getSimpleClassName(new Something()));
        assertEquals("nested", "Nested", TrafficMonitorFilter.getSimpleClassName(new Something.Nested()));
        assertEquals("object", "Object", TrafficMonitorFilter.getSimpleClassName(new Object()));
        assertEquals("object anon subclass", "Object$", TrafficMonitorFilter.getSimpleClassName(new Object(){}));
    }

    private static class Something {

        public static class Nested {
        }
    }

}