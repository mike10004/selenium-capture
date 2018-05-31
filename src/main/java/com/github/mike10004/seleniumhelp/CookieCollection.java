package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;

public interface CookieCollection {

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
    ImmutableList<DeserializableCookie> makeUltimateCookieList();

}
