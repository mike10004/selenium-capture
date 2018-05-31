package com.github.mike10004.seleniumhelp;

import com.google.common.net.HttpHeaders;
import org.apache.http.Header;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.message.BasicHeader;

import javax.annotation.Nullable;
import java.net.URL;
import java.time.Instant;
import java.util.List;

interface SetCookieHeaderParser {

    List<Cookie> parse(Header header, CookieOrigin origin, @Nullable Instant creationDate) throws MalformedCookieException;

    default List<Cookie> parse(String setCookieHeaderValue, URL originatingRequestUrl, @Nullable Instant creationDate) throws MalformedCookieException {
        CookieUtility.ParsedCookieOrigin parsedCookieOrigin = CookieUtility.getInstance().buildCookieOrigin(originatingRequestUrl);
        return parse(setCookieHeaderValue, parsedCookieOrigin.origin, creationDate);
    }

    default List<Cookie> parse(String setCookieHeaderValue, CookieOrigin origin, @Nullable Instant creationDate) throws MalformedCookieException {
        Header header = new BasicHeader(HttpHeaders.SET_COOKIE, setCookieHeaderValue);
        return parse(header, origin, creationDate);
    }

    static SetCookieHeaderParser create() {
        return new RFC6265SetCookieHeaderParser(CookieAttributeHandlers.getDefaultAttributeHandlers());
    }

}
