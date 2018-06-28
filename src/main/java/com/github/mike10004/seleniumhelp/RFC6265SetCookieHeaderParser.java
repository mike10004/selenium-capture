package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableMap;
import org.apache.http.FormattedHeader;
import org.apache.http.Header;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.CommonCookieAttributeHandler;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieAttributeHandler;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SM;
import org.apache.http.message.ParserCursor;
import org.apache.http.message.TokenParser;
import org.apache.http.util.CharArrayBuffer;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Cookie Parser based on {@link org.apache.http.impl.cookie.RFC6265CookieSpec}.
 */
class RFC6265SetCookieHeaderParser implements SetCookieHeaderParser {
    private final static char PARAM_DELIMITER  = ';';
    private final static char EQUAL_CHAR       = '=';

    // IMPORTANT!
    // These private static variables must be treated as immutable and never exposed outside this class
    private static final BitSet TOKEN_DELIMS = TokenParser.INIT_BITSET(EQUAL_CHAR, PARAM_DELIMITER);
    private static final BitSet VALUE_DELIMS = TokenParser.INIT_BITSET(PARAM_DELIMITER);

    private final ImmutableMap<String, CookieAttributeHandler> attribHandlerMap_;
    private final TokenParser tokenParser = TokenParser.INSTANCE;

    public RFC6265SetCookieHeaderParser(Iterable<CommonCookieAttributeHandler> handlers) {
        ImmutableMap.Builder<String, CookieAttributeHandler> b = ImmutableMap.builder();
        for (final CommonCookieAttributeHandler handler: handlers) {
            b.put(handler.getAttributeName().toLowerCase(Locale.ROOT), handler);
        }
        this.attribHandlerMap_ = b.build();
    }

    static String getDefaultDomain(final CookieOrigin origin) {
        return origin.getHost();
    }

    protected DeserializableCookie.Builder buildCookie(CookieOrigin origin, String name, String value, @Nullable Instant creationDate) {
        return DeserializableCookie.builder(name, value)
                .domain(getDefaultDomain(origin))
                .path(getDefaultPath(origin))
                .creationDate(creationDate);
    }

    public List<Cookie> parse(final Header header, final CookieOrigin origin, final @Nullable Instant creationDate) throws MalformedCookieException {
        requireNonNull(header, "Header");
        requireNonNull(origin, "Cookie origin");
        if (!header.getName().equalsIgnoreCase(SM.SET_COOKIE)) {
            throw new MalformedCookieException("Unrecognized cookie header: '" + header.toString() + "'");
        }
        final CharArrayBuffer buffer;
        final ParserCursor cursor;
        if (header instanceof FormattedHeader) {
            buffer = ((FormattedHeader) header).getBuffer();
            cursor = new ParserCursor(((FormattedHeader) header).getValuePos(), buffer.length());
        } else {
            final String s = header.getValue();
            if (s == null) {
                throw new MalformedCookieException("Header value is null");
            }
            buffer = new CharArrayBuffer(s.length());
            buffer.append(s);
            cursor = new ParserCursor(0, buffer.length());
        }
        final String name = tokenParser.parseToken(buffer, cursor, TOKEN_DELIMS);
        if (name.length() == 0) {
            return Collections.emptyList();
        }
        if (cursor.atEnd()) {
            return Collections.emptyList();
        }
        final int valueDelim = buffer.charAt(cursor.getPos());
        cursor.updatePos(cursor.getPos() + 1);
        if (valueDelim != '=') {
            throw new MalformedCookieException("Cookie value is invalid: '" + header.toString() + "'");
        }
        final String value = tokenParser.parseValue(buffer, cursor, VALUE_DELIMS);
        if (!cursor.atEnd()) {
            cursor.updatePos(cursor.getPos() + 1);
        }
        final DeserializableCookie.Builder cookie = buildCookie(origin, name, value, creationDate);

        final Map<String, String> attribMap = new LinkedHashMap<>();
        while (!cursor.atEnd()) {
            final String paramName = tokenParser.parseToken(buffer, cursor, TOKEN_DELIMS)
                    .toLowerCase(Locale.ROOT);
            String paramValue = null;
            if (!cursor.atEnd()) {
                final int paramDelim = buffer.charAt(cursor.getPos());
                cursor.updatePos(cursor.getPos() + 1);
                if (paramDelim == EQUAL_CHAR) {
                    paramValue = tokenParser.parseToken(buffer, cursor, VALUE_DELIMS);
                    if (!cursor.atEnd()) {
                        cursor.updatePos(cursor.getPos() + 1);
                    }
                }
            }
            attribMap.put(paramName, paramValue);
        }
        // Ignore 'Expires' if 'Max-Age' is present
        if (attribMap.containsKey(ClientCookie.MAX_AGE_ATTR)) {
            attribMap.remove(ClientCookie.EXPIRES_ATTR);
        }
        cookie.attributes(attribMap);
        for (final Map.Entry<String, String> entry: attribMap.entrySet()) {
            final String paramName = entry.getKey().toLowerCase(Locale.ROOT);
            final String paramValue = entry.getValue();
            final CookieAttributeHandler handler = this.attribHandlerMap_.get(paramName);
            if (handler != null) {
                handler.parse(cookie, paramValue);
            }
        }

        return Collections.singletonList(cookie.build());
    }

    static String getDefaultPath(final CookieOrigin origin) {
        String defaultPath = origin.getPath();
        int lastSlashIndex = defaultPath.lastIndexOf('/');
        if (lastSlashIndex >= 0) {
            if (lastSlashIndex == 0) {
                //Do not remove the very first slash
                lastSlashIndex = 1;
            }
            defaultPath = defaultPath.substring(0, lastSlashIndex);
        }
        return defaultPath;
    }

}
