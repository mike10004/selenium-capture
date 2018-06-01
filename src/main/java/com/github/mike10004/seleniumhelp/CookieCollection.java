package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import java.util.Comparator;
import java.util.function.Function;

/**
 * Interface of collections of cookies received while web browsing. Multiple cookies are received
 * during browsing, and a normal client keeps the most recent (according to creation date).
 * This interface defines methods to retain cookies based on criteria other than the creation date.
 */
public interface CookieCollection {

    /**
     * Creates a list of cookies that would ultimately be retained by a browser. Browsers
     * insert or update cookies as they are received, and cookies with the same
     * domain/name/path values are overwritten.
     *
     * <p>Reference: https://www.sitepoint.com/3-things-about-cookies-you-may-not-know/</p>
     * @return a copy of the list of cookies with most recent receipt timestamps for each
     *         domain/name/path triplet
     */
    default ImmutableList<DeserializableCookie> makeUltimateCookieList() {
        return makeCookieList(orderingByCreationDate());
    }

    static Ordering<DeserializableCookie> orderingByCreationDate() {
        return Ordering.natural().onResultOf(DeserializableCookie::getCreationDate).nullsFirst();
    }

    default ImmutableList<DeserializableCookie> makeCookieList(Comparator<? super DeserializableCookie> ordering) {
        return makeCookieList(anykey -> ordering);
    }

    /**
     * Creates a list of cookies with unique domain/name/path triplets.
     * @param orderingFactory factory providing comparators for selecting which cookie to return for each domain/name/path triplet.
     * @return a list of cookies
     */
    ImmutableList<DeserializableCookie> makeCookieList(Function<? super CookieKey, Comparator<? super DeserializableCookie>> orderingFactory);

}
