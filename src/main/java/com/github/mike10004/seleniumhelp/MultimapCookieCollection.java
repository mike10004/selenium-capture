package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.net.HttpHeaders;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import org.apache.http.cookie.MalformedCookieException;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class MultimapCookieCollection implements CookieCollection {

    private final ImmutableMultimap<CookieKey, DeserializableCookie> cookies;

    private MultimapCookieCollection(Multimap<CookieKey, DeserializableCookie> cookiesByEntry) {
        this.cookies = ImmutableMultimap.copyOf(cookiesByEntry);
    }

    ImmutableCollection<DeserializableCookie> getAllReceived() {
        return cookies.values();
    }

    static CookieCollection build(Iterable<DeserializableCookie> cookies) {
        return new MultimapCookieCollection(buildCookieKeyMultimap(cookies));
    }

    @Override
    public ImmutableList<DeserializableCookie> makeUltimateCookieList() {
        ImmutableList.Builder<DeserializableCookie> b = ImmutableList.builder();
        cookies.asMap().forEach((key, cookieList) -> {
            b.add(COOKIES_BY_CREATION_DATE.max(cookieList));
        });
        return b.build();
    }

    private static final Ordering<DeserializableCookie> COOKIES_BY_CREATION_DATE = Ordering.natural().onResultOf(DeserializableCookie::getCreationDate);

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
