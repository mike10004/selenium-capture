package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Ordering;
import com.google.common.math.LongMath;
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

    protected HarAnalysis(Har har) {
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
        return MultimapCookieCollection.build(headerValues, cookieSpec);
    }

    private static class IndexedEntry {

        public final int index;
        public final HarEntry entry;
        public final long sequence;

        public IndexedEntry(int index, HarEntry entry, long sequence) {
            this.index = index;
            this.entry = entry;
            this.sequence = sequence;
        }

        public static IndexedEntry from(int index, HarEntry entry) {
            long sequence = Long.MIN_VALUE;
            Date entryStarted = entry.getStartedDateTime();
            long entryStartedMs = -1;
            if (entryStarted != null) {
                entryStartedMs = entryStarted.getTime();
            }
            if (entryStartedMs >= 0) {
                long entryTimeMs = entry.getTime();
                if (entryTimeMs > 0) {
                    sequence = LongMath.saturatedAdd(entryStartedMs, entryTimeMs);
                }
            }
            return new IndexedEntry(index, entry, sequence);
        }

        private static final Ordering<IndexedEntry> ORDERING_BY_SEQUENCE = Ordering.natural().onResultOf(entry -> entry.sequence);

        public static Ordering<IndexedEntry> orderingBySequence() {
            return ORDERING_BY_SEQUENCE;
        }
    }

    private Stream<HarEntry> findEntriesWithSetCookieHeaders() {
        Stream<HarEntry> entriesWithCookieHeaders = har.getLog().getEntries().stream()
                .filter(entryPredicate(any(), input -> input != null && input.getHeaders().stream().anyMatch((hnvp) -> HttpHeaders.SET_COOKIE.equalsIgnoreCase(hnvp.getName()))));
        return entriesWithCookieHeaders;
    }

    private static Predicate<HarEntry> entryPredicate(final Predicate<HarRequest> requestPredicate, final Predicate<HarResponse> responsePredicate) {
        return harEntry -> harEntry != null && requestPredicate.test(harEntry.getRequest()) && responsePredicate.test(harEntry.getResponse());
    }

    private static <T> Predicate<T> any() {
        return x -> true;
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
