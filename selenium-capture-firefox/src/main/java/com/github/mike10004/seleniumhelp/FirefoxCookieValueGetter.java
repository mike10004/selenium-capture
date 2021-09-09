package com.github.mike10004.seleniumhelp;

import java.util.Map;

public interface FirefoxCookieValueGetter {

    Object getValueBySqlColumnName(Map<String, Object> explodedCookie, String sqlColumnName);

    void supplementSqlFields(Map<String, Object> explodedCookie, Map<String, String> sqlRow);
}
