package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import javax.annotation.Nullable;
import java.text.Format;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MapMatcher<K, V> extends BaseMatcher<Map<K, V>> {

    private Set<Object> keysMissingFromActual = ImmutableSet.of(), extraKeysInActual = ImmutableSet.of();
    private Map<Object, Pair<Pair<Object, Class<?>>, Pair<Object, Class<?>>>> mismatches = new LinkedHashMap<>();

    private final Map<?, ?> expected;
    private Map<?, ?> actual;

    public MapMatcher(Map<K, V> expected) {
        this.expected = expected;
    }

    private static @Nullable
    Class<?> classOf(Object object) {
        return object == null ? null : object.getClass();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(Object item) {
        if (item == null) {
            return expected == null;
        }
        if (expected == null) {
            return false;
        }
        if (!(item instanceof Map)) {
            return false;
        }
        actual = (Map<?, ?>) item;
        keysMissingFromActual = Sets.difference((Set<Object>)expected.keySet(), actual.keySet());
        extraKeysInActual = Sets.difference((Set<Object>)actual.keySet(), expected.keySet());
        Set<Object> commonKeys = Sets.intersection((Set<Object>)expected.keySet(), actual.keySet());
        for (Object key : commonKeys) {
            Object expectedValue = transformExpectedValue(key, expected.get(key));
            Object actualValue = transformActualValue(key, actual.get(key));
            if (!isIgnoreValueEquality(key, expectedValue, actualValue)) {
                boolean eq = equals(expectedValue, actualValue);
                if (!eq) {
                    Pair<Object, Class<?>> e = Pair.of(explainExpectedValue(key, expectedValue), classOf(expectedValue));
                    Pair<Object, Class<?>> a = Pair.of(explainActualValue(key, actualValue), classOf(actualValue));
                    mismatches.put(key, Pair.of(e, a));
                }
            }
        }
        return keysMissingFromActual.isEmpty() && extraKeysInActual.isEmpty() && mismatches.isEmpty();
    }

    protected boolean isIgnoreValueEquality(Object key, Object expectedValue, Object actualValue) {
        return false;
    }

    protected Object transformValue(Object key, Object value) {
        return value;
    }

    protected Object transformActualValue(Object key, Object value) {
        return transformValue(key, value);
    }

    protected Object transformExpectedValue(Object key, Object value) {
        return transformValue(key, value);
    }

    protected boolean equals(Object expectedValue, Object actualValue) {
        return java.util.Objects.deepEquals(expectedValue, actualValue);
    }

    protected Object explainExpectedValue(Object key, Object value) {
        return explainValue(key, value);
    }

    protected Object explainActualValue(Object key, Object value) {
        return explainValue(key, value);
    }

    protected Object explainValue(Object key, Object value) {
        return value;
    }

    @Override
    public void describeTo(Description description) {
        if (actual == null) {
            description.appendText(null);
            return;
        }
        boolean started = false;
        if (!keysMissingFromActual.isEmpty()) {
            description.appendText("keys in expected but not actual: ").appendValue(keysMissingFromActual);
            started = true;
        }
        if (!extraKeysInActual.isEmpty()) {
            if (started) {
                description.appendText("; ");
            }
            description.appendText("keys in actual but not expected: ").appendValue(extraKeysInActual);
            started = true;
        }
        if (!mismatches.isEmpty()) {
            if (started) {
                description.appendText("; ");
            }
            description.appendText("unequal values: ").appendValue(mismatches);
        }
    }

    public static <K, V> MapMatcher<K, V> expectingWithTruncatedMillis(Map<K, V> expected) {
        return new DateTruncatingMapMatcher<>(expected, Calendar.SECOND);
    }

    public static <K, V> MapMatcher<K, V> expecting(Map<K, V> expected) {
        return new MapMatcher<>(expected);
    }

    public static class DateTruncatingMapMatcher<K, V> extends MapMatcher<K, V> {

        private final int mostSignificantField;
        private final Format explanationFormat;

        public DateTruncatingMapMatcher(Map<K, V> expected, int mostSignificantField) {
            super(expected);
            this.mostSignificantField = mostSignificantField;
            explanationFormat = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT;
        }

        @Override
        protected Object transformValue(Object key, Object value) {
            if (value instanceof Date) {
                return DateUtils.truncate((Date) value, mostSignificantField);
            }
            return value;
        }

        @Override
        protected Object explainValue(Object key, Object value) {
            if (value instanceof Date) {
                String formatted = explanationFormat.format(value);
                return String.format("%s (%d)", formatted, ((Date) value).getTime());
            }
            return value;
        }
    }


}
