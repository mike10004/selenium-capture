package com.github.mike10004.seleniumhelp;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.CharSource;
import org.junit.Test;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class FirefoxCookieRowTransformTest {

    @Test
    public void apply() throws Exception {
        FirefoxCookieRowTransform conv = new Firefox68CookieRowTransform();
        Map<String, Object> input = ExampleCookieSource.asExplodedCookie();
        Map<String, String> expected = Iterables.getOnlyElement(Csvs.readRowMaps(CharSource.wrap(ExampleCookieSource.csvText), Csvs.headersFromFirstRow()));
        Map<String, String> actual = conv.apply(Firefox68CookieImporter.getImportInfo().columnNames(), input);
        final Set<String> ignores = ImmutableSet.of("originAttributes");
        assertThat("imploded", actual, new MapMatcher.DateTruncatingMapMatcher<String, String>(expected, Calendar.SECOND) {
            @Override
            protected Object transformValue(Object key, Object value) {
                if (value != null && ("lastAccessed".equals(key) || "creationTime".equals(key))) {
                    BigInteger num;
                    try {
                        num = new BigInteger(value.toString());
                    } catch (NumberFormatException e) {
                        System.err.format("failed to convert %s -> %s to BigInteger", key, value);
                        return value;
                    }
                    long roundedToTheNearest1000 = num.add(BigInteger.valueOf(500)).divide(BigInteger.valueOf(1000)).longValueExact();
                    return roundedToTheNearest1000;
                } else {
                    return value;
                }
            }

            @Override
            protected boolean isIgnoreValueEquality(Object key, Object expectedValue, Object actualValue) {
                return ignores.contains(key);
            }
        });
        assertEquals("originAttributes",
                Firefox68CookieRowTransform.ATTRIB_JOINER.join(ImmutableMap.of("^appId", "4294967294", "domain", ".google.com")),
                actual.get("originAttributes"));
    }

    @Test
    public void retainNameAndValue() throws Exception {

        Map<String, Object> explosion = ImmutableMap.<String, Object>builder()
                .put("name", "foo")
                .put("value", "bar")
                .build();
        Map<String, String> row = new Firefox68CookieRowTransform().apply(Firefox68CookieImporter.getImportInfo().columnNames(), explosion);
        assertNotNull(row);
        assertEquals("name in " + row, "foo", row.get("name"));
        assertEquals("value in " + row, "bar", row.get("value"));
    }

}