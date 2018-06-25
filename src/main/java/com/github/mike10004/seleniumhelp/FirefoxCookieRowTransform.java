package com.github.mike10004.seleniumhelp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Converter;
import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InternetDomainName;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Function;

public class FirefoxCookieRowTransform {

    private static java.util.function.Function<Map<String, Object>, Object> valueByKey(final String key) {
        return map -> map.get(key);
    }

    private static final CharMatcher dot = CharMatcher.is('.');

    @VisibleForTesting
    static MapJoiner ATTRIB_JOINER = Joiner.on(';').withKeyValueSeparator('=');

    private static final ImmutableMap<String, Function<Map<String, Object>, Object>> _sqliteFieldToCookieValueGetterMap = ImmutableBiMap.<String, Function<Map<String, Object>, Object>>builder()
            .put("path", valueByKey("cookiePath"))
            .put("expiry", valueByKey("cookieExpiryDate"))
            .put("creationTime", valueByKey("creationDate"))
            .put("originAttributes", valueByKey("attribs"))
            .put("isHttpOnly", valueByKey("httpOnly"))
            .put("baseDomain", map -> {
                String domain = (String) map.get(DeserializableCookie.FIELD_DOMAIN);
                if (domain != null) {
                    domain = dot.trimLeadingFrom(domain);
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
                            .map(Entry::getValue)
                            .findFirst()
                            .orElse(null);
                    return value;
                }
                return null;
            })
            .build();

    private final Converter<Map<String, Object>, Map<String, String>> converter;

    public FirefoxCookieRowTransform() {
        converter = new Converter<Map<String, Object>, Map<String, String>>() {
            @Override
            protected Map<String, String> doForward(Map<String, Object> explodedCookie) {
                 return FirefoxCookieRowTransform.this.apply(explodedCookie);
            }

            @Override
            protected Map<String, Object> doBackward(Map<String, String> stringStringMap) {
                throw new UnsupportedOperationException("doBackward not supported");
            }
        };
    }

    public java.util.function.Function<Map<String, Object>, Map<String, String>> asFunction() {
        return converter::convert;
    }

    public Converter<Map<String, Object>, Map<String, String>> asConverter() {
        return converter;
    }

    protected Function<Map<String, Object>, Object> getCookieValueGetterBySqlFieldName(String sqlFieldName) {
        return _sqliteFieldToCookieValueGetterMap.getOrDefault(sqlFieldName, map -> map.get(sqlFieldName));
    }

    public Map<String, String> apply(Map<String, Object> explodedCookie) {
        Map<String, String> sqlRowMap = new TreeMap<>();
        for (String sqlFieldName : FirefoxCookieDb.SQLITE_COLUMN_NAMES) {
            sqlRowMap.put(sqlFieldName, "");
            Object value = getCookieValueGetterBySqlFieldName(sqlFieldName).apply(explodedCookie);
            if (value != null) {
                String valueStr = stringifyCookieValue(value, sqlFieldName);
                sqlRowMap.put(sqlFieldName, valueStr);
            }
        }
        supplementSqlFields(explodedCookie, sqlRowMap);
        return sqlRowMap;
    }

    private static int getCookieToSqlDateConversionExponentBySqlFieldName(String sqlFieldName) {
        switch (sqlFieldName) {
            case "expiry":
                return -EXPIRY_EXPONENT;
            case "lastAccessed":
                return -LAST_ACCESSED_EXPONENT;
            case "creationTime":
                return -CREATED_EXPONENT;
            default:
                throw new IllegalArgumentException("exponent unknown for " + sqlFieldName);
        }
    }

    protected String stringifyCookieValue(Object cookieFieldValue, String sqlFieldName) {
        if (cookieFieldValue instanceof Map) {
            return ATTRIB_JOINER.join((Map)cookieFieldValue);
        } else if (cookieFieldValue instanceof Instant) {
            Instant instant = (Instant) cookieFieldValue;
            BigInteger millis = BigInteger.valueOf(instant.toEpochMilli());
            int exponent = getCookieToSqlDateConversionExponentBySqlFieldName(sqlFieldName);
            final long value;
            if (exponent >= 0) {
                BigInteger factor = BigInteger.TEN.pow(exponent);
                value = millis.multiply(factor).longValueExact();
            } else {
                BigInteger denominator = BigInteger.TEN.pow(-exponent);
                value = millis.divide(denominator).longValueExact();
            }
            return String.valueOf(value);
        } else if (cookieFieldValue instanceof Boolean) {
            return ((Boolean)cookieFieldValue).booleanValue() ? "1" : "0";
        } else {
            return cookieFieldValue.toString();
        }
    }

    protected void supplementSqlFields(Map<String, Object> explodedCookie, Map<String, String> sqlRow) {
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

    private static final int CREATED_EXPONENT = -3;
    private static final int LAST_ACCESSED_EXPONENT = -3;
    private static final int EXPIRY_EXPONENT = 3;

}
