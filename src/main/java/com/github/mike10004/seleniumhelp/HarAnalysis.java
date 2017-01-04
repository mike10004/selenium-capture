package com.github.mike10004.seleniumhelp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.math.LongMath;
import com.google.common.net.HttpHeaders;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import net.lightbody.bmp.core.har.HarRequest;
import net.lightbody.bmp.core.har.HarResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.cookie.MalformedCookieException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class HarAnalysis {

    private static class CookieSpecHolder {
        private final static FlexibleCookieSpec cookieSpec_ = FlexibleCookieSpec.getDefault();

        public static FlexibleCookieSpec get() {
            return FlexibleCookieSpec.getDefault();
        }
    }

    private final Har har;

    private HarAnalysis(Har har) {
        this.har = checkNotNull(har);
    }

    public static HarAnalysis of(Har har) {
        return new HarAnalysis(har);
    }

    public CookieCollection findCookies() {
        return findCookies(CookieSpecHolder.get());
    }

    CookieCollection findCookies(final FlexibleCookieSpec cookieSpec) {
        return findCookies(cookieSpec, HarEntry::getStartedDateTime);
    }

    public CookieCollection findCookies(Function<HarEntry, Date> creationDateGetter) {
        return findCookies(CookieSpecHolder.get(), creationDateGetter);
    }

    CookieCollection findCookies(final FlexibleCookieSpec cookieSpec, Function<HarEntry, Date> creationDateGetter) {
        Stream<HarEntry> headerValues = findEntriesWithSetCookieHeaders();
        return CookieCollection.build(headerValues, cookieSpec);
    }

    public static class CookieCollection {

        private final ImmutableMultimap<HarEntry, DeserializableCookie> cookiesByEntry;

        @VisibleForTesting
        CookieCollection(Multimap<HarEntry, DeserializableCookie> cookiesByEntry) {
            this.cookiesByEntry = ImmutableMultimap.copyOf(cookiesByEntry);
        }

        @VisibleForTesting
        static long getEntryResponseInstant(HarEntry entry) {
            return LongMath.checkedAdd(entry.getStartedDateTime().getTime(), entry.getTime());
        }

        public static CookieCollection build(Stream<HarEntry> headerValues) {
            return build(headerValues, CookieSpecHolder.get());
        }

        static CookieCollection build(Stream<HarEntry> headerValues, final FlexibleCookieSpec cookieSpec) {
            ImmutableMultimap.Builder<HarEntry, DeserializableCookie> m = ImmutableMultimap.builder();
            headerValues.forEach(entry -> {
                List<DeserializableCookie> cookies = makeCookiesFromEntry(cookieSpec, entry);
                m.putAll(entry, cookies);
            });
            return new CookieCollection(m.build());
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
            return entries.sorted(Ordering.<Long>natural().onResultOf(CookieCollection::getEntryResponseInstant));
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

        static boolean debug;
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
            System.out.format("Set-Cookie: %s%n", headerValue);
            try {
                Stream<DeserializableCookie> cookieStream = cookieSpec.parse(headerValue, originUrl).stream()
                        .map(x -> (DeserializableCookie) x);
                cookieStream.forEach(cookie -> {
                    cookie.setCreationDate(requestDate);
                    cookies.add(cookie);
                });
            } catch (MalformedCookieException e) {
                throw new IllegalArgumentException(e);
            }
        });
        return cookies;
    }

    public Stream<HarEntry> findEntriesWithSetCookieHeaders() {
        Stream<HarEntry> entriesWithCookieHeaders = har.getLog().getEntries().stream()
                .filter(entryPredicate(anyRequest(), input -> input != null && input.getHeaders().stream().anyMatch((hnvp) -> HttpHeaders.SET_COOKIE.equalsIgnoreCase(hnvp.getName()))));
        return entriesWithCookieHeaders;
    }

    public Stream<Pair<HarRequest, Stream<String>>> findCookieHeaderValues() {
        Stream<HarEntry> entriesWithCookieHeaders = findEntriesWithSetCookieHeaders();
        Stream<Pair<HarRequest, HarResponse>> interactionsWithCookieHeaders = entriesWithCookieHeaders.map(entryToInteraction());
        Stream<Pair<HarRequest, Stream<String>>> mappedInteractions = interactionsWithCookieHeaders.map(rightTransform(responseHeaderTransform(HttpHeaders.SET_COOKIE)));
        return mappedInteractions;
    }

    public static Function<HarResponse, Stream<String>> responseHeaderTransform(final String requiredHeaderName) {
        checkNotNull(requiredHeaderName);
        return input -> {
            Stream<HarNameValuePair> stream = input.getHeaders().stream().filter(harNameValuePair -> requiredHeaderName.equalsIgnoreCase(harNameValuePair.getName()));
            return stream.map(HarNameValuePair::getValue);
        };
    }

    private static <L, R> Predicate<Pair<L, R>> conjoinedPairPredicate(final Predicate<? super L> leftPredicate, final Predicate<? super R> rightPredicate) {
        return input -> input != null && leftPredicate.test(input.getLeft()) && rightPredicate.test(input.getRight());
    }

    public static Function<HarEntry, Pair<HarRequest, HarResponse>> entryToInteraction() {
        return input -> Pair.of(input.getRequest(), input.getResponse());
    }

    public static Predicate<HarEntry> entryPredicate(final Predicate<HarRequest> requestPredicate, final Predicate<HarResponse> responsePredicate) {
        return harEntry -> harEntry != null && requestPredicate.test(harEntry.getRequest()) && responsePredicate.test(harEntry.getResponse());
    }

    public Stream<Pair<HarRequest, HarResponse>> asInteractions() {
        return har.getLog().getEntries().stream().map(entryToInteraction());
    }

    public Stream<Pair<HarRequest, HarResponse>> asInteractions(Predicate<HarRequest> requestPredicate, Predicate<HarResponse> responsePredicate) {
        return har.getLog().getEntries().stream()
                .filter(entryPredicate(requestPredicate, responsePredicate))
                .map(entryToInteraction());
    }

    public static Predicate<HarRequest> anyRequest() {
        return harRequest -> true;
    }

    public static Predicate<HarResponse> anyResponse() {
        return harResponse -> true;
    }

    public static <L1, R1, L2, R2> Function<Pair<L1, R1>, Pair<L2, R2>> pairTransform(final Function<L1, L2> left, final Function<R1, R2> right) {
        return input -> Pair.of(left.apply(input.getLeft()), right.apply(input.getRight()));
    }

    public static <L, R1, R2> Function<Pair<L, R1>, Pair<L, R2>> rightTransform(final Function<R1, R2> right) {
        return pairTransform(Function.identity(), right);
    }

    public static <L1, L2, R> Function<Pair<L1, R>, Pair<L2, R>> leftTransform(final Function<L1, L2> left) {
        return pairTransform(left, Function.identity());
    }

}
