package com.github.mike10004.seleniumhelp;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;

import java.net.URL;
import java.util.Date;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

public class BasicCookieFilter implements CookieFilter {

    private final CookieUtility cookieUtility;
    private final CookieSpec cookieSpec;

    public BasicCookieFilter() {
        this(CookieAttributeHandlers.makeNonparsingCookieSpec());
    }

    public BasicCookieFilter(CookieSpec cookieSpec) {
        this.cookieUtility = CookieUtility.getInstance();
        this.cookieSpec = cookieSpec;
    }

    @Override
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
    @Override
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
        java.util.function.Predicate<Cookie> matchesOrigin = makeApacheOriginPredicate(cookieOrigin);
        java.util.function.Predicate<Cookie> notExpiredAndMatchesOrigin = notExpired.and(matchesOrigin);
        return notExpiredAndMatchesOrigin;
    }

    @Override
    public Predicate<Cookie> makeApacheOriginPredicate(CookieOrigin cookieOrigin) {
        return input -> {
            checkNotNull(input, "input");
            boolean matches = cookieSpec.match(input, cookieOrigin);
            return matches;
        };
    }

}
