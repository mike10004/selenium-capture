package com.github.mike10004.seleniumhelp;

import com.google.common.base.CharMatcher;
import com.google.common.base.Converter;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.math.LongMath;
import com.google.common.net.InternetDomainName;
import com.google.gson.internal.LinkedTreeMap;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class SqliteRowMapToExplodedCookieConverter extends Converter<Map<String, String>, Map<String, Object>> {

    private static java.util.function.Function<Map<String, Object>, Object> valueByKey(final String key) {
        return map -> map.get(key);
    }

    private static final CharMatcher dot = CharMatcher.is('.');

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

//        protected static String getSqliteValueByCookieFieldName(String sqliteFieldName, Map<String, Object> cookieMap) {
//            String sqliteFieldName = sqliteFieldToCookieFieldMap.inverse().get(cookieFieldName);
//            if (sqliteFieldName == null) {
//                sqliteFieldName = cookieFieldName;
//            }
//            return sqliteFieldName;
//        }
//
//        protected static String getCookieFieldName(String sqliteFieldName) {
//            String cookieFieldName = sqliteFieldToCookieFieldMap.get(sqliteFieldName);
//            if (cookieFieldName == null) {
//                return sqliteFieldName;
//            }
//            return cookieFieldName;
//        }

    @Override
    protected Map<String, Object> doForward(Map<String, String> row) {
//            Map<String, Object> intermediateRep = new TreeMap<>();
//            for (String sqliteFieldName : row.keySet()) {
//                String sqliteValue = row.get(sqliteFieldName);
//                if (!Strings.isNullOrEmpty(sqliteValue)) {
//                    Object cookieValue = getValueTransform(sqliteFieldName).apply(sqliteValue);
//                    String cookieFieldName = getCookieFieldName(sqliteFieldName);
//                    intermediateRep.put(cookieFieldName, cookieValue);
//                }
//            }
//            return intermediateRep;
        throw new UnsupportedOperationException("not implemented");
    }

    protected Function<Map<String, Object>, Object> getCookieValueGetterBySqlFieldName(String sqlFieldName) {
        return _sqliteFieldToCookieValueGetterMap.getOrDefault(sqlFieldName, map -> map.get(sqlFieldName));
    }

    @Override
    protected Map<String, String> doBackward(Map<String, Object> explodedCookie) {
        Map<String, String> sqlRowMap = new TreeMap<>();
        for (String sqlFieldName : FirefoxCookieDb.sqliteColumnNames) {
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
        } else if (cookieFieldValue instanceof Date) {
            BigInteger millis = BigInteger.valueOf(((Date)cookieFieldValue).getTime());
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
    }

    static com.google.common.base.Function<Long, Long> createMillisecondsAdjuster(final int exponent) {
        return new com.google.common.base.Function<Long, Long>() {
            @Override
            public Long apply(Long value) {
                final long powerOf10 = LongMath.pow(10, Math.abs(exponent));
                final boolean divide = exponent < 0;
                long adjustedValue;
                if (divide) {
                    adjustedValue = Math.round((double)value / (double)powerOf10);
                } else {
                    adjustedValue = LongMath.checkedMultiply(value, powerOf10);
                }
                return adjustedValue;
            }
        };
    }

    private static com.google.common.base.Function<Object, Object> integerToDateFunction(final com.google.common.base.Function<Long, Long> millisecondsAdjuster) {

        return new com.google.common.base.Function<Object, Object>(){

            private final Supplier<Calendar> calendarSupplier = new Supplier<Calendar>(){
                @Override
                public Calendar get() {
                    return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                }
            };

            @Override
            public Date apply(Object input) {
                input = numberify(input);
                long value = ((Number)input).longValue();
                long adjustedValue = millisecondsAdjuster.apply(value).longValue();
                Calendar cal = calendarSupplier.get();
                cal.setTimeInMillis(adjustedValue);
                return cal.getTime();
            }
        };
    }

    private static final int CREATED_EXPONENT = -3;
    private static final int LAST_ACCESSED_EXPONENT = -3;
    private static final int EXPIRY_EXPONENT = 3;

    private static final com.google.common.base.Function<Object, Object> integerToBooleanFunction = new com.google.common.base.Function<Object, Object>() {
        @Override
        public Boolean apply(Object input) {
            input = numberify(input);
            return ((Number)input).longValue() != 0L;
        }
    };

    private static Number numberify(Object object) {
        checkNotNull(object, "object");
        if (object instanceof Number) {
            return (Number) object;
        }
        String str = object.toString();
        return Long.valueOf(str);
    }

    private static final com.google.common.base.Function<Object, Object> numberifier = SqliteRowMapToExplodedCookieConverter::numberify;

    private static final com.google.common.base.Function<Object, Object> attributesParser = new com.google.common.base.Function<Object, Object>() {
        @Override
        public LinkedTreeMap<String, String> apply(Object input) {
            checkArgument(input instanceof String, "expect string: %s", input);
            Matcher m = Pattern.compile("(\\^\\w+)=(\\S+)").matcher((String) input);
            int start = 0;
            LinkedTreeMap<String, String> attribs = new LinkedTreeMap<>();
            while (m.find(start)) {
                String key = m.group(1);
                String value = m.group(2);
                attribs.put(key, value);
                start = m.end();
            }
            return attribs;
        }
    };

    private static Supplier<ImmutableMap<String, com.google.common.base.Function<Object, Object>>> sqliteValueToCookieValueTransforms
            = Suppliers.memoize(new Supplier<ImmutableMap<String, com.google.common.base.Function<Object, Object>>>() {
        @Override
        public ImmutableMap<String, com.google.common.base.Function<Object, Object>> get() {
            return ImmutableMap.<String, com.google.common.base.Function<Object, Object>>builder()
                    .put("originAttributes", attributesParser)
                    .put("creationTime", integerToDateFunction(createMillisecondsAdjuster(CREATED_EXPONENT)))
                    .put("lastAccessed", integerToDateFunction(createMillisecondsAdjuster(LAST_ACCESSED_EXPONENT)))
                    .put("expiry", integerToDateFunction(createMillisecondsAdjuster(EXPIRY_EXPONENT)))
                    .put("isSecure", integerToBooleanFunction)
                    .put("isHttpOnly", integerToBooleanFunction)
                    .put("inBrowserElement", numberifier)
                    .put("appId", numberifier)
                    .put("id", numberifier)
                    .build();
        }
    });

    private static com.google.common.base.Function<Object, Object> getValueTransform(String sqliteFieldName) {
        com.google.common.base.Function<Object, Object> function = sqliteValueToCookieValueTransforms.get().get(sqliteFieldName);
        if (function == null) {
            return Functions.identity();
        }
        return function;
    }

}
