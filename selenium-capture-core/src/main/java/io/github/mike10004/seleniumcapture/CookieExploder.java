package io.github.mike10004.seleniumcapture;

import java.util.Map;

/**
 * Interface of a service that converts a cookie into a key-value map.
 * Map keys are equal to the {@link DeserializableCookie} class instance
 * fields.
 * Values in the map retain the types they have in the cookie class instance.
 * Any entry whose value is the default for the type is removed from the
 * map that is returned.
 */
public interface CookieExploder {

    Map<String, Object> explode(DeserializableCookie cookie);

}

