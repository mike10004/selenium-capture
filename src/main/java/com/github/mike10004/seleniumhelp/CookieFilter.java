package com.github.mike10004.seleniumhelp;

import org.apache.http.cookie.CookieOrigin;

import java.net.URL;
import java.util.Date;
import java.util.function.Predicate;

public interface CookieFilter {

    Predicate<org.apache.http.cookie.Cookie> makeApacheOriginPredicate(final CookieOrigin cookieOrigin);

    Predicate<org.openqa.selenium.Cookie> makeSeleniumPredicate(final URL url, final Date date);

    Predicate<org.apache.http.cookie.Cookie> makeApachePredicate(final URL url, final Date date);

}

