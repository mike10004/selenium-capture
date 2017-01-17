package com.github.mike10004.seleniumhelp;

import com.google.common.net.HttpHeaders;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import net.lightbody.bmp.core.har.HarRequest;
import net.lightbody.bmp.core.har.HarResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.cookie.MalformedCookieException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class HarAnalysis {

    private final Har har;

    private HarAnalysis(Har har) {
        this.har = checkNotNull(har);
    }

    public static HarAnalysis of(Har har) {
        return new HarAnalysis(har);
    }

    public CookieCollection findCookies() {
        return findCookies(FlexibleCookieSpec.getDefault());
    }

    CookieCollection findCookies(final FlexibleCookieSpec cookieSpec) {
        return findCookies(cookieSpec, HarEntry::getStartedDateTime);
    }

    public CookieCollection findCookies(Function<HarEntry, Date> creationDateGetter) {
        return findCookies(FlexibleCookieSpec.getDefault(), creationDateGetter);
    }

    CookieCollection findCookies(final FlexibleCookieSpec cookieSpec, Function<HarEntry, Date> creationDateGetter) {
        Stream<HarEntry> headerValues = findEntriesWithSetCookieHeaders();
        return CookieCollection.build(headerValues, cookieSpec);
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
