package com.github.mike10004.seleniumhelp;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.impl.cookie.DefaultCookieSpecProvider;
import org.apache.http.protocol.BasicHttpContext;

import java.net.URL;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

public class CookieFilter {

    private final CookieUtility cookieUtility;
    private final CookieSpec cookieSpec;

    public CookieFilter() {
        this(createDefaultCookieSpec());
    }

    protected static CookieSpec createDefaultCookieSpec() {
        return new FlexibleCookieSpec(new DefaultCookieSpecProvider().create(new BasicHttpContext()));
    }

    public CookieFilter(CookieSpec cookieSpec) {
        this.cookieUtility = CookieUtility.getInstance();
        this.cookieSpec = cookieSpec;
    }

    public java.util.function.Predicate<org.openqa.selenium.Cookie> makeSeleniumPredicate(final URL url, final Date date) {
        final java.util.function.Predicate<Cookie> apachePredicate = makeApachePredicate(url, date);
        return new java.util.function.Predicate<org.openqa.selenium.Cookie>() {

            private final SeleniumCookieConverter converter = new SeleniumCookieConverter();

            @Override
            public boolean test(org.openqa.selenium.Cookie cookie) {
                return apachePredicate.test(converter.convert(cookie));
            }
        };
    }

    /**
     * Creates a predicate that filters cookies based on the given URL and a date.
     * @param url the URL on which to filter the returned cookies
     * @param date date to use when checking whether cookies are expired ("now" is appropriate)
     * @return a predicate that filters cookies
     */
    public java.util.function.Predicate<Cookie> makeApachePredicate(final URL url, final Date date) {
        Pair<CookieOrigin, URL> cookieOriginAndNormalizedUrl = cookieUtility.buildCookieOrigin(url);
        final URL normalizedUrl = cookieOriginAndNormalizedUrl.getRight();
        final String host = normalizedUrl.getHost();
        // URLs like "about:blank" don't have cookies and we need to catch these
        // cases here before HttpClient complains
        if (host.isEmpty()) {
            return cookie -> false;
        }
        final CookieOrigin cookieOrigin = cookieOriginAndNormalizedUrl.getLeft();
        java.util.function.Predicate<Cookie> notExpired = cookieUtility.notExpiredOn(date);
        java.util.function.Predicate<Cookie> matchesOrigin = originMatcher(cookieOrigin);
        java.util.function.Predicate<Cookie> notExpiredAndMatchesOrigin = notExpired.and(matchesOrigin);
        return notExpiredAndMatchesOrigin;
    }

    private java.util.function.Predicate<Cookie> originMatcher(final CookieOrigin cookieOrigin) {
        return input -> {
            checkNotNull(input, "input");
            boolean matches = cookieSpec.match(input, cookieOrigin);
            return matches;
        };
    }
}
