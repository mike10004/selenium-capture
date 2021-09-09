package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;

import java.util.Comparator;
import java.util.function.Function;

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
    public ImmutableList<DeserializableCookie> makeCookieList(Function<? super CookieKey, Comparator<? super DeserializableCookie>> comparatorFactory) {
        requireNonNull(comparatorFactory, "comparator");
        ImmutableList.Builder<DeserializableCookie> b = ImmutableList.builder();
        cookies.asMap().forEach((key, cookieList) -> {
            Comparator<? super DeserializableCookie> comparator = comparatorFactory.apply(key);
            b.add(Ordering.from(comparator).max(cookieList));
        });
        return b.build();
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
