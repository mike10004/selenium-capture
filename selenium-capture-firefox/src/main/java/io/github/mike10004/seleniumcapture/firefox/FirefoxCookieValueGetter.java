package io.github.mike10004.seleniumcapture.firefox;

import java.util.Map;

public interface FirefoxCookieValueGetter {

    Object getValueBySqlColumnName(Map<String, Object> explodedCookie, String sqlColumnName);

    void supplementSqlFields(Map<String, Object> explodedCookie, Map<String, String> sqlRow);
}
