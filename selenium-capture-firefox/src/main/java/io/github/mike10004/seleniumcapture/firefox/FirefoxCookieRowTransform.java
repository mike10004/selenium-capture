package io.github.mike10004.seleniumcapture.firefox;

import java.util.Map;

/**
 * Interface of a service that transforms an exploded cookie
 * into a map that defines a row of a database table.
 */
public interface FirefoxCookieRowTransform {

    /**
     * Transforms an exploded cookie into a map that represents a row in a sqlite database table.
     * @param explodedCookie cookie
     * @return row
     */
    Map<String, String> apply(Map<String, Object> explodedCookie);

}

