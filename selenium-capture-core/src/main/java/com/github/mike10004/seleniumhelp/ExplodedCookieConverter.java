package com.github.mike10004.seleniumhelp;

import java.util.Map;

/**
 * Interface of a service that converts a cookie into a key-value map.
 * Values in the map retain the types they have in the cookie class instance.
 * Entries that have default values are removed.
 */
public interface ExplodedCookieConverter {

    Map<String, Object> explode(DeserializableCookie cookie);

}

