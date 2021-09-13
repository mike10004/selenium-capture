package io.github.mike10004.seleniumcapture.firefox;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import io.github.mike10004.seleniumcapture.CookieExploder;
import io.github.mike10004.seleniumcapture.DeserializableCookie;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Objects.requireNonNull;

public class FirefoxCookieRowTransformBase implements FirefoxCookieRowTransform {

    protected static java.util.function.Function<Map<String, Object>, Object> valueByKey(final String key) {
        return map -> map.get(key);
    }

    protected static final CharMatcher dot = CharMatcher.is('.');

    @VisibleForTesting
    static Joiner.MapJoiner ATTRIB_JOINER = Joiner.on(';').withKeyValueSeparator('=');

    private final CookieExploder cookieExploder;
    private final ImmutableList<String> columnNames;
    private final FirefoxCookieValueGetter cookieValueGetter;

    public FirefoxCookieRowTransformBase(CookieExploder cookieExploder, List<String> columnNames, FirefoxCookieValueGetter cookieValueGetter) {
        this.columnNames = ImmutableList.copyOf(columnNames);
        this.cookieExploder = requireNonNull(cookieExploder);
        this.cookieValueGetter = requireNonNull(cookieValueGetter);
    }

    @Override
    public Map<String, String> apply(DeserializableCookie cookie) {
        Map<String, Object> explodedCookie = cookieExploder.explode(cookie);
        Map<String, String> sqlRowMap = new TreeMap<>();
        for (String sqlFieldName : columnNames) {
            Object value = cookieValueGetter.getValueBySqlColumnName(explodedCookie, sqlFieldName);
            if (value != null) {
                String valueStr = stringifyCookieValue(value, sqlFieldName);
                sqlRowMap.put(sqlFieldName, valueStr);
            }
        }
        cookieValueGetter.supplementSqlFields(explodedCookie, sqlRowMap);
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
            return ATTRIB_JOINER.join((Map<?, ?>) cookieFieldValue);
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
            return ((Boolean) cookieFieldValue).booleanValue() ? "1" : "0";
        } else {
            return cookieFieldValue.toString();
        }
    }

    private static final int CREATED_EXPONENT = -3;
    private static final int LAST_ACCESSED_EXPONENT = -3;
    private static final int EXPIRY_EXPONENT = 3;

}
