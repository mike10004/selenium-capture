package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;
import org.apache.http.Header;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.cookie.CommonCookieAttributeHandler;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;
import org.apache.http.impl.cookie.BasicDomainHandler;
import org.apache.http.impl.cookie.BasicPathHandler;
import org.apache.http.impl.cookie.BasicSecureHandler;
import org.apache.http.impl.cookie.CookieSpecBase;
import org.apache.http.impl.cookie.DefaultCookieSpecProvider;
import org.apache.http.impl.cookie.LaxExpiresHandler;
import org.apache.http.impl.cookie.LaxMaxAgeHandler;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.Args;

import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

class FlexibleCookieSpec extends CookieSpecBase {

    private final CookieSpec delegate;
    private final CookieParser cookieParser;

    private static final CommonCookieAttributeHandler[] handlersArray = {
            new BasicPathHandler(),       // similar to RFC6265LaxSpec
            new BetterDomainHandler(),  // with the exception of domain handling
            new LaxMaxAgeHandler(),
            new BasicSecureHandler(),
            new LaxExpiresHandler()
    };
    private static final ImmutableList<CommonCookieAttributeHandler> handlersList = ImmutableList.copyOf(handlersArray);

    public FlexibleCookieSpec(CookieSpec delegate) {
        this(delegate, handlersArray);
    }

    static ImmutableList<CommonCookieAttributeHandler> getDefaultAttributeHandlers() {
        return handlersList;
    }

    private FlexibleCookieSpec(CookieSpec delegate, CommonCookieAttributeHandler...attributeHandlers) {
        super(attributeHandlers);
        this.delegate = checkNotNull(delegate);
        cookieParser = new CookieParser(Arrays.asList(attributeHandlers));
    }

    public static FlexibleCookieSpec getDefault() {
        return new FlexibleCookieSpec(new DefaultCookieSpecProvider().create(new BasicHttpContext()));
    }

    public List<Cookie> parse(String setCookieHeaderValue, URL originUrl, Date creationDate) throws MalformedCookieException {
        return parse(setCookieHeaderValue, CookieUtility.getInstance().buildCookieOrigin(originUrl).origin, creationDate);
    }

    public List<Cookie> parse(String setCookieHeaderValue, CookieOrigin origin, Date creationDate) throws MalformedCookieException {
        Header header = new BasicHeader(HttpHeaders.SET_COOKIE, setCookieHeaderValue);
        return parse(header, origin);
    }

    @Override
    public int getVersion() {
        return delegate.getVersion();
    }

    @Override
    public List<Cookie> parse(Header header, CookieOrigin origin) throws MalformedCookieException {
        List<DeserializableCookie> cookies = cookieParser.parse(header, origin);
        return cookies.stream().map(x -> x).collect(Collectors.toList());
    }

    @Override
    public List<Header> formatCookies(List<Cookie> cookies) {
        return delegate.formatCookies(cookies);
    }

    @Override
    public Header getVersionHeader() {
        return delegate.getVersionHeader();
    }

    private static class BetterDomainHandler extends BasicDomainHandler {

        @Override
        public void parse(final SetCookie cookie, final String value)
                throws MalformedCookieException {
            // does nothing
        }

        static boolean domainMatch(final String domain, final String host) {
            if (InetAddressUtils.isIPv4Address(host) || InetAddressUtils.isIPv6Address(host)) {
                return false;
            }
            final String normalizedDomain = domain.startsWith(".") ? domain.substring(1) : domain;
            if (host.endsWith(normalizedDomain)) {
                final int prefix = host.length() - normalizedDomain.length();
                // Either a full match or a prefix ending with a '.'
                if (prefix == 0) {
                    return true;
                }
                if (prefix > 1 && host.charAt(prefix - 1) == '.') {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean match(final Cookie cookie, final CookieOrigin origin) {
            Args.notNull(cookie, "Cookie");
            Args.notNull(origin, "Cookie origin");
            final String host = origin.getHost();
            String domain = cookie.getDomain();
            if (domain == null) {
                return false;
            }
            if (domain.startsWith(".")) {
                domain = domain.substring(1);
            }
            domain = domain.toLowerCase(Locale.ROOT);
            if (host.equals(domain)) {
                return true;
            }
            return domainMatch(domain, host);
        }


    }
}
