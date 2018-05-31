package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

class MultimapCookieCollection implements CookieCollection {

    private final ImmutableMultimap<CookieKey, DeserializableCookie> cookies;

    private MultimapCookieCollection(Multimap<CookieKey, DeserializableCookie> cookiesByEntry) {
        this.cookies = ImmutableMultimap.copyOf(cookiesByEntry);
    }

    static CookieCollection build(Iterable<DeserializableCookie> cookies) {
        return new MultimapCookieCollection(buildCookieKeyMultimap(cookies));
    }

    @Override
    public ImmutableList<DeserializableCookie> makeCookieList(Comparator<? super DeserializableCookie> comparator) {
        requireNonNull(comparator, "comparator");
        ImmutableList.Builder<DeserializableCookie> b = ImmutableList.builder();
        cookies.asMap().forEach((key, cookieList) -> {
            b.add(Ordering.from(comparator).max(cookieList));
        });
        return b.build();
    }

    private static class CookieKey {
        public final String domain;
        public final String name;
        public final String path;

        private CookieKey(String domain, String name, String path) {
            this.domain = requireNonNull(domain);
            this.name = requireNonNull(name);
            this.path = requireNonNull(path);
        }

        public static CookieKey from(@Nullable String domain, @Nullable String name, @Nullable String path) {
            //noinspection ConstantConditions
            return new CookieKey(Strings.nullToEmpty(domain), Strings.nullToEmpty(name), MoreObjects.firstNonNull(path, "/"));
        }

        @Override
        public String toString() {
            return "CookieKey{" +
                    "domain='" + domain + '\'' +
                    ", name='" + name + '\'' +
                    ", path='" + path + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CookieKey cookieKey = (CookieKey) o;
            return Objects.equals(domain, cookieKey.domain) &&
                    Objects.equals(name, cookieKey.name) &&
                    Objects.equals(path, cookieKey.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(domain, name, path);
        }

        public static CookieKey from(DeserializableCookie cookie) {
            return CookieKey.from(cookie.getBestDomainProperty(), cookie.getName(), cookie.getPath());
        }
    }

    private static Multimap<CookieKey, DeserializableCookie> buildCookieKeyMultimap(Iterable<DeserializableCookie> cookies) {
        Multimap<CookieKey, DeserializableCookie> updatingMap = ArrayListMultimap.create();
        cookies.forEach(cookie -> {
            CookieKey key = CookieKey.from(cookie);
            updatingMap.put(key, cookie);
        });
        return updatingMap;
    }

}
