package com.github.mike10004.seleniumhelp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.math.LongMath;
import com.google.common.net.HttpHeaders;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.cookie.MalformedCookieException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public class MultimapCookieCollection implements CookieCollection {
    private final ImmutableMultimap<HarEntry, DeserializableCookie> cookiesByEntry;

    @VisibleForTesting
    MultimapCookieCollection(Multimap<HarEntry, DeserializableCookie> cookiesByEntry) {
        this.cookiesByEntry = ImmutableMultimap.copyOf(cookiesByEntry);
    }

    @VisibleForTesting
    static long getEntryResponseInstant(HarEntry entry) {
        return LongMath.checkedAdd(entry.getStartedDateTime().getTime(), entry.getTime());
    }

    static CookieCollection build(Stream<HarEntry> headerValues) {
        return build(headerValues, FlexibleCookieSpec.getDefault());
    }

    static CookieCollection build(Stream<HarEntry> headerValues, final FlexibleCookieSpec cookieSpec) {
        ImmutableMultimap.Builder<HarEntry, DeserializableCookie> m = ImmutableMultimap.builder();
        headerValues.forEach(entry -> {
            List<DeserializableCookie> cookies = makeCookiesFromEntry(cookieSpec, entry);
            m.putAll(entry, cookies);
        });
        return new MultimapCookieCollection(m.build());
    }

    /**
     * Creates a list of cookies that would ultimately be retained by a browser. Browsers
     * insert or update cookies as they are received, and cookies with the same name/domain/path values
     * are overwritten. This method sorts the HAR entries by the time responses were received
     * by the browser and builds a list of each cookie that was received last among those with the
     * same domain, name, and path.
     *
     * <p>Reference: https://www.sitepoint.com/3-things-about-cookies-you-may-not-know/</p>
     * @return a copy of the list of cookies with most recent receipt timestamps for each
     *         domain/name/path triplet
     */
    public ImmutableList<DeserializableCookie> makeUltimateCookieList() {
        return ImmutableList.copyOf(buildUltimateCookieMap().values());
    }

    @VisibleForTesting
    static Stream<HarEntry> sortHarEntriesByResponseInstant(Stream<HarEntry> entries) {
        return entries.sorted(Ordering.<Long>natural().onResultOf(MultimapCookieCollection::getEntryResponseInstant));
    }

    @VisibleForTesting
    Map<Triple<String, String, String>, DeserializableCookie> buildUltimateCookieMap() {
        Map<Triple<String, String, String>, DeserializableCookie> updatingMap = new TreeMap<>(Ordering.<Triple<String, String, String>>natural());
        Stream<HarEntry> entriesByResponseInstant = sortHarEntriesByResponseInstant(cookiesByEntry.keySet().stream());
        entriesByResponseInstant.forEach(harEntry -> {
            for (DeserializableCookie cookie : cookiesByEntry.get(harEntry)) {
                Triple<String, String, String> cookieLabel = Triple.of(MoreObjects.firstNonNull(cookie.getBestDomainProperty(), ""),
                        MoreObjects.firstNonNull(cookie.getName(), ""),
                        MoreObjects.firstNonNull(cookie.getPath(), "/"));
                updatingMap.put(cookieLabel, cookie);
            }
        });
        return updatingMap;
    }

    private static List<DeserializableCookie> makeCookiesFromEntry(final FlexibleCookieSpec cookieSpec, final HarEntry entry) {
        URL originUrl;
        Date requestDate = entry.getStartedDateTime();
        try {
            originUrl = new URL(entry.getRequest().getUrl());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        Stream<String> headerValues = entry.getResponse().getHeaders().stream()
                .filter(header -> HttpHeaders.SET_COOKIE.equalsIgnoreCase(header.getName()))
                .map(HarNameValuePair::getValue);
        final List<DeserializableCookie> cookies = new ArrayList<>();
        headerValues.forEach(headerValue -> {
            try {
                Stream<DeserializableCookie> cookieStream = cookieSpec.parse(headerValue, originUrl, requestDate).stream()
                        .map(x -> (DeserializableCookie) x);
                cookieStream.forEach(cookies::add);
            } catch (MalformedCookieException e) {
                throw new IllegalArgumentException(e);
            }
        });
        return cookies;
    }

}
