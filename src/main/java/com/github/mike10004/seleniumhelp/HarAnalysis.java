package com.github.mike10004.seleniumhelp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.net.HttpHeaders;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import net.lightbody.bmp.core.har.HarPostData;
import net.lightbody.bmp.core.har.HarRequest;
import net.lightbody.bmp.core.har.HarResponse;
import org.apache.http.cookie.MalformedCookieException;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
        return findCookies(SetCookieHeaderParser.create());
    }

    CookieCollection findCookies(final SetCookieHeaderParser cookieSpec) {
        Stream<HarEntry> headerValues = findEntriesWithSetCookieHeaders();
        List<DeserializableCookie> cookies = new ArrayList<>();
        headerValues.forEach(entry -> {
            cookies.addAll(makeCookiesFromEntry(cookieSpec, entry));
        });
        return MultimapCookieCollection.build(cookies);
    }

    private Stream<HarEntry> findEntriesWithSetCookieHeaders() {
        Stream<HarEntry> entriesWithCookieHeaders = har.getLog().getEntries().stream()
                .filter(ENTRY_HAS_SET_COOKIE_HEADER_IN_RESPONSE);
        return entriesWithCookieHeaders;
    }

    private static final Predicate<HarEntry> ENTRY_HAS_SET_COOKIE_HEADER_IN_RESPONSE = new Predicate<HarEntry>() {
        @Override
        public boolean test(HarEntry entry) {
            HarResponse input = null;
            if (entry != null) {
                input = entry.getResponse();
            }
            return input != null && input.getHeaders().stream().anyMatch(header -> HttpHeaders.SET_COOKIE.equalsIgnoreCase(header.getName()));
        }
    };

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

    private static Instant getResponseMoment(HarEntry entry) {
        Instant requestInstant = entry.getStartedDateTime().toInstant();
        long entryTimeMs = entry.getTime();
        Instant responseInstant = requestInstant.plus(entryTimeMs, ChronoUnit.MILLIS);
        return responseInstant;
    }

    @VisibleForTesting
    static List<DeserializableCookie> makeCookiesFromEntry(final SetCookieHeaderParser cookieSpec, final HarEntry entry) {
        URL originUrl;
        Instant creationDate = getResponseMoment(entry);
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
                List<org.apache.http.cookie.Cookie> parsed = cookieSpec.parse(headerValue, originUrl, creationDate);
                Stream<DeserializableCookie> cookieStream = parsed.stream()
                        .map(x -> (DeserializableCookie) x);
                cookieStream.forEach(cookies::add);
            } catch (MalformedCookieException e) {
                throw new IllegalArgumentException(e);
            }
        });
        return cookies;
    }

}
