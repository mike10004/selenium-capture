package io.github.mike10004.seleniumcapture.firefox;

import io.github.mike10004.seleniumcapture.DeserializableCookie;

import java.util.Map;

/**
 * Interface of a service that transforms an exploded cookie
 * into a map that defines a row of a database table.
 */
public interface FirefoxCookieRowTransform {

    /**
     * Transforms a cookie into a map that represents a record in a sqlite database table.
     * @param cookie cookie
     * @return record
     */
    Map<String, String> apply(DeserializableCookie cookie);

}

