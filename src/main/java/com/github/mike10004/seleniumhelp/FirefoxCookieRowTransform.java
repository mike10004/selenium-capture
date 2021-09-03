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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Function;

public interface FirefoxCookieRowTransform {

    /**
     * Transforms an exploded cookie into a map that represents a row in a sqlite table.
     * @param explodedCookie cookie
     * @return row
     */
    Map<String, String> apply(List<String> columnNames, Map<String, Object> explodedCookie);

    Converter<Map<String, Object>, Map<String, String>> asConverter(List<String> columnNames);

}

