package io.github.mike10004.seleniumcapture.firefox;

import java.util.Map;

/**
 * Interface of a service that gets values from an exploded cookie
 * for use in transforming it into a database record.
 */
public interface FirefoxCookieValueGetter {

    /**
     * Gets the value of a database table field from an exploded cookie.
     * @param explodedCookie cookie
     * @param sqlColumnName field name
     * @return value
     */
    Object getValueBySqlColumnName(Map<String, Object> explodedCookie, String sqlColumnName);

    /**
     * Fills the values of fields in a database table record from a cookie.
     * @param explodedCookie cookie
     * @param sqlRow record
     */
    void supplementSqlFields(Map<String, Object> explodedCookie, Map<String, String> sqlRow);

}
