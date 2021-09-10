package com.github.mike10004.seleniumhelp;

import javax.annotation.Nullable;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class MapUtils {

    private MapUtils() {}

    // not thread safe; invokes containsKey before get
    @Nullable
    public static <V> V getValueByCaseInsensitiveKey(@Nullable Map<String, V> attrs, String key) {
        requireNonNull(key, "key");
        if (attrs == null) {
            return null;
        }
        // first try exact key match; iterate through entries only if no exact key match exists
        if (attrs.containsKey(key)) {
            return attrs.get(key);
        }
        return attrs.entrySet().stream()
                .filter(entry -> key.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
