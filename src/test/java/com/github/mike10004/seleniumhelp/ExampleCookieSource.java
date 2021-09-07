package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ExampleCookieSource {

    public static String getCsvText_FF68() { return "id,baseDomain,originAttributes,name,value," +
            "host,path,expiry,lastAccessed,creationTime,isSecure,isHttpOnly,appId,inBrowserElement\n" +
            ",google.com,^appId=4294967294,NID," +
            "91=vxeiuscvhp3k-93Ot_QHkPuD6WqVrYxoQTW7nJS492yvvIF929NYgb5B5avkkwFiVDEEisVvNGRWkITyWmPCvI2CHooVAgHQ-3fprSHSzQDNR1gmpJ6abGVjx7OZZfJc," +
            ".google.com,/,1496330743,1480519543925563,1480519543925563,0,1,,";}

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

    // id
    // baseDomain
    // originAttributes
    // name
    // value
    // host
    // path
    // expiry
    // lastAccessed
    // creationTime
    // isSecure
    // isHttpOnly
    // appId
    // inBrowserElement" +

    public static Map<String, String> raw_ff68() {
        return ImmutableMap.<String, String>builder()
                .put("baseDomain", "google.com")
                .put("originAttributes", "^appId=4294967294")
                .put("name", name)
                .put("value", value)
                .put("host", originHost)
                .put("path", path)
                .put("expiry", "1496330743")
                .put("lastAccessed", "1480519543925563")
                .put("creationTime", "1480519543925563")
                .put("isSecure", "0")
                .put("isHttpOnly", "1")
                .put("appId", "")
                .put("inBrowserElement", "")
                .build();
    }

    public static Map<String, String> raw_ff91() {
        return ImmutableMap.<String, String>builder()
                .put("originAttributes", "^appId=4294967294")
                .put("name", name)
                .put("value", value)
                .put("host", originHost)
                .put("path", path)
                .put("expiry", "1496330743")
                .put("lastAccessed", "1480519543925563")
                .put("creationTime", "1480519543925563")
                .put("isSecure", "0")
                .put("isHttpOnly", "1")
                .put("inBrowserElement", "")
                .put("sameSite", "0")
                .put("rawSameSite", "")
                .put("schemeMap", "")
                .build();
    }

    public static String getCsvText_FF91() {
//        Map<String, String> raw = raw_ff91();
//        List<Map.Entry<String, String>> entries = ImmutableList.copyOf(raw.entrySet());
//        List<String> headers = entries.stream().map(Map.Entry::getKey).collect(Collectors.toList());
//        List<String> values = entries.stream().map(Map.Entry::getValue).collect(Collectors.toList());
        throw new UnsupportedOperationException("not yet implemented");
    }

    public static ImmutableMap<String, Object> asExplodedCookie() {
        return _asExplodedCookie(s -> false);
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
