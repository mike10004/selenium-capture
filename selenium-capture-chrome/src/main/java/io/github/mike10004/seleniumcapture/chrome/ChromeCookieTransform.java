package io.github.mike10004.seleniumcapture.chrome;

import com.github.mike10004.chromecookieimplant.ChromeCookie;
import io.github.mike10004.seleniumcapture.DeserializableCookie;
import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Service class that transforms regular cookies into Chrome cookies.
 * Takes instances of {@link DeserializableCookie} as input and
 * constructs equivalent instances of {@link ChromeCookie}.
 */
class ChromeCookieTransform {

    public ChromeCookie transform(DeserializableCookie input) {
        String url = fabricateUrlFromDomain(input.getBestDomainProperty(), input.isSecure(), input.getPath());
        ChromeCookie output = ChromeCookie.builder(url)
                .name(input.getName())
                .value(input.getValue())
                .domain(input.getBestDomainProperty())
                .path(input.getPath())
                .expirationDate(convertInstantToSecondsSinceEpoch(input.getExpiryInstant()))
                .secure(input.isSecure())
                .httpOnly(input.isHttpOnly())
                .sameSite(ChromeCookie.SameSiteStatus.no_restriction)
                .build();
        return output;
    }

    @Nullable
    private static BigDecimal convertInstantToSecondsSinceEpoch(@Nullable Instant instant) {
        if (instant != null) {
            return BigDecimal.valueOf(instant.toEpochMilli()).scaleByPowerOfTen(-3);
        }
        return null;
    }

    private static CharMatcher dotMatcher = CharMatcher.is('.');
    private static CharMatcher slashMatcher = CharMatcher.is('/');

    protected String fabricateUrlFromDomain(String domain, boolean secure, String path) {
        if (Strings.isNullOrEmpty(domain)) {
            LoggerFactory.getLogger(ChromeCookieTransform.class).warn("input cookie has no domain, so no URL can be fabricated; chrome will not like this");
            return "";
        }
        domain = dotMatcher.trimLeadingFrom(domain);
        String scheme = secure ? "https" : "http";
        path = slashMatcher.trimLeadingFrom(Strings.nullToEmpty(path));
        return scheme + "://" + domain + "/" + path;
    }

}
