package com.github.mike10004.seleniumhelp;

import java.time.Instant;
import java.util.function.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;

import java.util.Date;

public class ExampleCookieSource {

    public static final String csvText = "id,baseDomain,originAttributes,name,value,host,path,expiry,lastAccessed,creationTime,isSecure,isHttpOnly,appId,inBrowserElement\n" +
            ",google.com,^appId=4294967294,NID,91=vxeiuscvhp3k-93Ot_QHkPuD6WqVrYxoQTW7nJS492yvvIF929NYgb5B5avkkwFiVDEEisVvNGRWkITyWmPCvI2CHooVAgHQ-3fprSHSzQDNR1gmpJ6abGVjx7OZZfJc,.google.com,/,1496330743,1480519543925563,1480519543925563,0,1,,";

    public static final String name = "NID";
    public static final String originHost = "www.google.com";
    public static final String baseDomain = "google.com";
    private static final String domainAttribute = ".google.com";
    public static final String path = "/";
    public static final String value = "91=vxeiuscvhp3k-93Ot_QHkPuD6WqVrYxoQTW7nJS492yvvIF929NYgb5B5avkkwFiVDEEisVvNGRWkITyWmPCvI2CHooVAgHQ-3fprSHSzQDNR1gmpJ6abGVjx7OZZfJc";
    public static final boolean secure = false;
    public static final long expiryDateMillisSinceEpoch = 1496330743000L;
    public static final long createdDateMillisSinceEpoch = 1480519543926L;
    public static final long accessDateMillisSinceEpoch = 1480519543926L;
    public static final boolean httpOnly = true;
    public static final ImmutableMap<String, String> attribs = ImmutableMap.of("^appId", "4294967294", "domain", domainAttribute);

    public static ImmutableMap<String, Object> asExplodedCookie() {
        return _asExplodedCookie(Predicates.alwaysFalse());
    }

    private static <K, V> void maybePut(Predicate<K> exclusionPredicate, ImmutableMap.Builder<K, V> builder, K key, V value) {
        if (!exclusionPredicate.test(key)) {
            builder.put(key, value);
        }
    }

    private static ImmutableMap<String, Object> _asExplodedCookie(Predicate<String> excludes) {
        ImmutableMap.Builder<String, Object> b = ImmutableMap.builder();
        maybePut(excludes, b, "name", name);
        maybePut(excludes, b, "attribs", attribs);
        maybePut(excludes, b, "value", value);
        maybePut(excludes, b, "cookieDomain", originHost);
        maybePut(excludes, b, "cookieExpiryDate", Instant.ofEpochMilli(expiryDateMillisSinceEpoch));
        maybePut(excludes, b, "cookiePath", path);
        maybePut(excludes, b, "isSecure", secure);
        maybePut(excludes, b, "httpOnly", httpOnly);
        maybePut(excludes, b, "creationDate", Instant.ofEpochMilli(createdDateMillisSinceEpoch));
        maybePut(excludes, b, "lastAccessed", Instant.ofEpochMilli(accessDateMillisSinceEpoch));
        return b.build();
    }

    public static DeserializableCookie asDeserializableCookie() {
        DeserializableCookie.Builder c = DeserializableCookie.builder(ExampleCookieSource.name, ExampleCookieSource.value);
        c.setSecure(ExampleCookieSource.secure);
        c.setPath(ExampleCookieSource.path);
        c.attributes(ExampleCookieSource.attribs);
        c.expiry(Instant.ofEpochMilli(ExampleCookieSource.expiryDateMillisSinceEpoch));
        c.setDomain(originHost);
        c.creationDate(Instant.ofEpochMilli((ExampleCookieSource.createdDateMillisSinceEpoch)));
        c.lastAccessed(Instant.ofEpochMilli((ExampleCookieSource.accessDateMillisSinceEpoch)));
        c.httpOnly(ExampleCookieSource.httpOnly);
        return c.build();
    }
}
