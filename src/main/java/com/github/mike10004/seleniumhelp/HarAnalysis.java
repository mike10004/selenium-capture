package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.net.HttpHeaders;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import net.lightbody.bmp.core.har.HarPostData;
import net.lightbody.bmp.core.har.HarRequest;
import net.lightbody.bmp.core.har.HarResponse;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
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

    public static Function<HarEntry, Pair<HarRequest, HarResponse>> entryToInteraction() {
        return input -> Pair.of(input.getRequest(), input.getResponse());
    }

    public static Predicate<HarEntry> entryPredicate(final Predicate<HarRequest> requestPredicate, final Predicate<HarResponse> responsePredicate) {
        return harEntry -> harEntry != null && requestPredicate.test(harEntry.getRequest()) && responsePredicate.test(harEntry.getResponse());
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

    public static String describe(HarRequest request) {
        if (request == null) {
            return "null";
        }
        return MoreObjects.toStringHelper(request)
                .add("method", request.getMethod())
                .add("url", request.getUrl())
                .add("headers.count", sizeOf(request.getHeaders()))
                .add("postData", request.getPostData())
                .toString();
    }

    private static int lengthOf(@Nullable String str) {
        return str == null ? -1 : str.length();
    }

    private static int sizeOf(@Nullable Collection<?> collection) {
        return collection == null ? -1 : collection.size();
    }

    public static String describe(HarResponse response) {
        if (response == null) {
            return "null";
        }
        return MoreObjects.toStringHelper(response)
                .add("status", response.getStatus())
                .add("headers.count", sizeOf(response.getHeaders()))
                .add("bodySize", response.getBodySize())
                .toString();
    }

    public static String describe(HarPostData postData) {
        if (postData == null) {
            return "null";
        }
        return MoreObjects.toStringHelper(postData)
                .add("params.size", sizeOf(postData.getParams()))
                .add("text.length", lengthOf(postData.getText()))
                .add("mimeType", postData.getMimeType())
                .toString();
    }
}
