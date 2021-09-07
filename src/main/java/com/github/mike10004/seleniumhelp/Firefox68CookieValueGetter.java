package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InternetDomainName;

import java.util.Map;
import java.util.function.Function;

public class Firefox68CookieValueGetter implements FirefoxCookieValueGetter {
    private static final ImmutableMap<String, Function<Map<String, Object>, Object>> VALUE_GETTER_MAP = ImmutableBiMap.<String, Function<Map<String, Object>, Object>>builder()
            .put("path", FirefoxCookieRowTransformBase.valueByKey("cookiePath"))
            .put("expiry", FirefoxCookieRowTransformBase.valueByKey("cookieExpiryDate"))
            .put("creationTime", FirefoxCookieRowTransformBase.valueByKey("creationDate"))
            .put("originAttributes", FirefoxCookieRowTransformBase.valueByKey("attribs"))
            .put("isHttpOnly", FirefoxCookieRowTransformBase.valueByKey("httpOnly"))
            .put("baseDomain", map -> {
                String domain = (String) map.get(DeserializableCookie.FIELD_DOMAIN);
                if (domain != null) {
                    domain = FirefoxCookieRowTransformBase.dot.trimLeadingFrom(domain);
                    if ("localhost".equals(domain)) {
                        return "localhost";
                    }
                    try {
                        InternetDomainName topPrivateDomain = InternetDomainName.from(domain).topPrivateDomain();
                        return topPrivateDomain.toString();
                    } catch (IllegalArgumentException ignore) {
                    }
                }
                return null;
            }).put("host", map -> {
                Map<?, ?> attrs = (Map<?, ?>) map.get(DeserializableCookie.FIELD_ATTRIBUTES);
                if (attrs != null) {
                    Object value = attrs.entrySet().stream()
                            .filter(entry -> "domain".equalsIgnoreCase(entry.getKey().toString()))
                            .map(Map.Entry::getValue)
                            .findFirst()
                            .orElse(null);
                    return value;
                }
                return null;
            })
            .build();

    private Function<Map<String, Object>, Object> getCookieValueGetterBySqlFieldName(String sqlFieldName) {
        return VALUE_GETTER_MAP.getOrDefault(sqlFieldName, map -> map.get(sqlFieldName));
    }

    @Override
    public Object getValueBySqlColumnName(Map<String, Object> explodedCookie, String sqlColumnName) {
        return getCookieValueGetterBySqlFieldName(sqlColumnName).apply(explodedCookie);
    }

    @Override
    public void supplementSqlFields(Map<String, Object> explodedCookie, Map<String, String> sqlRow) {
        if (!sqlRow.containsKey("isHttpOnly")) {
            sqlRow.put("isHttpOnly", "1"); // assume http only
        }
        if (!sqlRow.containsKey("host")) {
            String baseDomain = sqlRow.get("baseDomain");
            if ("localhost".equalsIgnoreCase(baseDomain)) {
                sqlRow.put("host", baseDomain);
            }
        }
    }

}
