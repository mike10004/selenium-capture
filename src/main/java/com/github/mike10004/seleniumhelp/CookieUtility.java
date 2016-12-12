package com.github.mike10004.seleniumhelp;

import com.google.common.base.CharMatcher;
import com.google.common.base.Converter;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HttpHeaders;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.Header;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.message.BasicHeader;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Cookie utility. Methods copied from HTMLUnit CookieManager. See that class for license.
 */
public class CookieUtility {

    private CookieUtility() {

    }

    private static final CookieUtility instance = new CookieUtility();

    public static CookieUtility getInstance() {
        return instance;
    }

    public static class CookieHash extends Triple<String, String, String> {

        private final ImmutableTriple<String, String, String> inner;

        public CookieHash(String domain, String name, String path) {
            this(ImmutableTriple.of(domain, name, path));
        }

        private CookieHash(ImmutableTriple<String, String, String> inner) {
            this.inner = inner;
        }

        @Override
        public String getLeft() {
            return inner.getLeft();
        }

        @Override
        public String getMiddle() {
            return inner.getMiddle();
        }

        @Override
        public String getRight() {
            return inner.getRight();
        }

        @Override
        public int compareTo(Triple<String, String, String> other) {
            return inner.compareTo(other);
        }

        @Override
        public boolean equals(Object obj) {
            return inner.equals(obj);
        }

        @Override
        public int hashCode() {
            return inner.hashCode();
        }

        @Override
        public String toString() {
            return inner.toString();
        }
    }

    public static CookieHash getIdentifier(Cookie apacheCookie) {
        return new CookieHash(checkNotNull(apacheCookie.getDomain(), "domain"),
                checkNotNull(apacheCookie.getName(), "name"),
                MoreObjects.firstNonNull(apacheCookie.getPath(), "/"));
    }

//    public Set<Cookie> filterApacheCookiesBySeleniumMatches(List<Cookie> apacheCookies, Iterable<org.openqa.selenium.Cookie> seleniumCookies) {
//        List<Cookie> matchingCookieList = new ArrayList<>();
//        for (org.openqa.selenium.Cookie seleniumCookie : seleniumCookies) {
//            Iterator<Cookie> matches = apacheCookies.stream().filter(apacheMatcher(seleniumCookie)).iterator();
//            if (matches.hasNext()) {
//                Cookie m = matches.next();
//                if (matches.hasNext()) {
//                    throw new IllegalArgumentException("more than one apache cookie matches " + seleniumCookie + "; all matching: " + matches);
//                }
//                matchingCookieList.add(m);
//            }
//        }
//        Map<CookieHash, Cookie> mapWithLatest = new HashMap<>(matchingCookieList.size());
//        for (Cookie cookie : matchingCookieList) {
//            mapWithLatest.put(getIdentifier(cookie), cookie);
//        }
//        Set<Cookie> cookieSet = ImmutableSet.copyOf(mapWithLatest.values());
//        return cookieSet;
//    }

//    public static Predicate<Cookie> apacheMatcher(final org.openqa.selenium.Cookie requiredParameters) {
//        return apacheMatcher(requiredParameters.getName(), requiredParameters.getValue(), requiredParameters.getDomain(), requiredParameters.getPath());
//    }

//    private static boolean isSameDomain(String domain, String apacheCookieDomainValue) {
//        return Objects.equals(domain, apacheCookieDomainValue)
//                || (!domain.isEmpty() && domain.charAt(0) == '.'
//                && domain.regionMatches(true, 1, apacheCookieDomainValue, 0, apacheCookieDomainValue.length()));
//    }

//    public static Predicate<Coo>
//
//    public static Predicate<Cookie> apacheMatcher(final String name, final String value, final String domain, final @Nullable String path) {
//        checkNotNull(name);
//        checkNotNull(value);
//        checkNotNull(domain);
//        return c -> c != null && name.equalsIgnoreCase(c.getName())
//                && value.equals(c.getValue())
//                && isSameDomain(domain, c.getDomain())
//                && Objects.equals(path, c.getPath());
//    }

    public static Function<String, Stream<Cookie>> headerToCookiesFunction(final CookieOrigin cookieOrigin, final CookieSpec cookieSpec) {
        return input -> {
            Header header = new BasicHeader(HttpHeaders.SET_COOKIE, input);
            try {
                return cookieSpec.parse(header, cookieOrigin).stream();
            } catch (MalformedCookieException e) {
                throw new IllegalArgumentException(e);
            }
        };
    }

    public static class ParsedCookieOrigin extends Pair<CookieOrigin, URL> {

        public final CookieOrigin origin;
        public final URL normalizedUrl;
        private final ImmutablePair<CookieOrigin, URL> delegate;

        public ParsedCookieOrigin(CookieOrigin origin, URL normalizedUrl) {
            this.origin = checkNotNull(origin);
            this.normalizedUrl = checkNotNull(normalizedUrl);
            this.delegate = ImmutablePair.of(origin, normalizedUrl);
        }

        @Override
        public CookieOrigin getLeft() {
            return delegate.getLeft();
        }

        @Override
        public URL getRight() {
            return delegate.getRight();
        }

        @Override
        public URL setValue(URL value) {
            return delegate.setValue(value);
        }

        @Override
        public URL getValue() {
            return delegate.getValue();
        }

        @Override
        public int compareTo(Pair<CookieOrigin, URL> other) {
            return delegate.compareTo(other);
        }

        @Override
        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    /**
     * Helper that builds a CookieOrigin.
     * @param url the url to be used
     * @return the new CookieOrigin and the normalized URL
     */
    public ParsedCookieOrigin buildCookieOrigin(final URL url) {
        final URL normalizedUrl = replaceForCookieIfNecessary(url);
        CookieOrigin origin = new CookieOrigin(
                normalizedUrl.getHost(),
                getPort(normalizedUrl),
                normalizedUrl.getPath(),
                "https".equals(normalizedUrl.getProtocol()));
        return new ParsedCookieOrigin(origin, normalizedUrl);
    }

    /**
     * Creates a predicate that filters expired cookies.
     * @param referenceDate the date to use for comparison (usually the current time) to the cookie's expiry date
     * @return a predicate that returns true for a cookie that is not expired by the given date
     */
    public synchronized Predicate<Cookie> notExpiredOn(final Date referenceDate) {
        checkNotNull(referenceDate, "date");
        return cookie -> {
            checkNotNull(cookie, "cookie");
            Date expiry = cookie.getExpiryDate();
            if (expiry == null) {
                return true;
            }
            boolean stillValid = expiry.after(referenceDate);
            return stillValid;
        };
    }

    /**
     * Gets the port of the URL.
     * This functionality is implemented here as protected method to allow subclass to change it
     * as workaround to <a href="http://code.google.com/p/googleappengine/issues/detail?id=4784">
     * Google App Engine bug 4784</a>.
     * @param url the URL
     * @return the port use to connect the server
     */
    protected int getPort(final URL url) {
        if (url.getPort() != -1) {
            return url.getPort();
        }
        return url.getDefaultPort();
    }

    /**
     * {@link CookieOrigin} doesn't like empty hosts and negative ports,
     * but these things happen if we're dealing with a local file.
     * This method allows us to work around this limitation in HttpClient by feeding it a bogus host and port.
     *
     * @param url the URL to replace if necessary
     * @return the replacement URL, or the original URL if no replacement was necessary
     */
    private URL replaceForCookieIfNecessary(URL url) {
        final String protocol = url.getProtocol();
        final boolean file = "file".equals(protocol);
        if (file) {
            try {
                url = getUrlWithNewHostAndPort(url, LOCAL_FILESYSTEM_DOMAIN, 0);
            }
            catch (final MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return url;
    }
    /** Workaround for domain of local files. */
    private static final String LOCAL_FILESYSTEM_DOMAIN = "LOCAL_FILESYSTEM";

    /**
     * Creates and returns a new URL identical to the specified URL, except using the specified host.
     * <p>See {@code com.gargoylesoftware.htmlunit.util.UrlUtils}.
     * @param u the URL on which to base the returned URL
     * @param newHost the new host to use in the returned URL
     * @param newPort the new port to use in the returned URL
     * @return a new URL identical to the specified URL, except using the specified host
     * @throws MalformedURLException if there is a problem creating the new URL
     */
    private static URL getUrlWithNewHostAndPort(final URL u, final String newHost, final int newPort)
            throws MalformedURLException {
        return createNewUrl(u.getProtocol(), u.getUserInfo(), newHost, newPort, u.getPath(), u.getRef(), u.getQuery());
    }

    /**
     * Creates a new URL based on the specified fragments.
     * <p>See {@code com.gargoylesoftware.htmlunit.util.UrlUtils}.
     * @param protocol the protocol to use (may not be {@code null})
     * @param userInfo the user info to use (may be {@code null})
     * @param host the host to use (may not be {@code null})
     * @param port the port to use (may be <tt>-1</tt> if no port is specified)
     * @param path the path to use (may be {@code null} and may omit the initial <tt>'/'</tt>)
     * @param ref the reference to use (may be {@code null} and must not include the <tt>'#'</tt>)
     * @param query the query to use (may be {@code null} and must not include the <tt>'?'</tt>)
     * @return a new URL based on the specified fragments
     * @throws MalformedURLException if there is a problem creating the new URL
     */
    private static URL createNewUrl(final String protocol, final String userInfo, final String host, final int port,
                                    final String path, final String ref, final String query) throws MalformedURLException {
        final StringBuilder s = new StringBuilder();
        s.append(protocol);
        s.append("://");
        if (userInfo != null) {
            s.append(userInfo).append("@");
        }
        s.append(host);
        if (port != -1) {
            s.append(":").append(port);
        }
        if (path != null && !path.isEmpty()) {
            if (!('/' == path.charAt(0))) {
                s.append("/");
            }
            s.append(path);
        }
        if (query != null) {
            s.append("?").append(query);
        }
        if (ref != null) {
            if (ref.isEmpty() || ref.charAt(0) != '#') {
                s.append("#");
            }
            s.append(ref);
        }

        final URL url = new URL(s.toString());
        return url;
    }

    private static String checkOnlyContains(String value, CharMatcher permitted) {
        if (!permitted.matchesAllOf(value)) {
            throw new IllegalArgumentException(String.format("value \"%s\" does not match %s", StringEscapeUtils.escapeJava(value), permitted));
        }
        return value;
    }

    private static CharMatcher legalNameChars = CharMatcher.noneOf("=");
    private static CharMatcher legalOtherChars = CharMatcher.noneOf(";");

    private static final SimpleDateFormat dateFormat;
    static {
        // Thu, 29-Nov-2018 19:22:24 GMT
        dateFormat = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    protected static String formatDateForHeader(Date date) {
        checkNotNull(date, "date");
        return dateFormat.format(date);
    }

    protected static @Nullable String getDomain(org.apache.http.cookie.Cookie c) {
        if (c instanceof ClientCookie) {
            ClientCookie d = (ClientCookie) c;
            @Nullable String dAttr = d.getAttribute("domain");
            if (dAttr != null) {
                return dAttr;
            }
        }
        return c.getDomain();
    }

    public String formatSetCookieHeader(DeserializableCookie c) {
        StringBuilder b = new StringBuilder(512);
        b.append(checkOnlyContains(c.getName(), legalNameChars));
        b.append('=');
        b.append(checkOnlyContains(c.getValue(), legalOtherChars));
        if (c.getExpiryDate() != null) {
            b.append("; Expires=").append(formatDateForHeader(c.getExpiryDate()));
        }
        @Nullable String domain = getDomain(c);
        if (domain != null) {
            b.append("; Domain=").append(domain);
        }
        if (c.getPath() != null) {
            b.append("; Path=").append(c.getPath());
        }
        if (c.isSecure()) {
            b.append("; Secure");
        }
        if (c.isHttpOnly()) {
            b.append("; HttpOnly");
        }
        return b.toString();
    }
}
