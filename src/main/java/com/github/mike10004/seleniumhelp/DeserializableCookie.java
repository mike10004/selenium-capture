/*
 * (c) 2016 ${COPYRIGHTER}
 */
package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.math.LongMath;
import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.SetCookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

/**
 * Class that represents a cookie that's easy to deserialize from json.
 * The purpose of this class is to substitute for 
 * {@link BasicClientCookie}, which does not have
 * a no-args constructor, during deserialization by a 
 * {@link Gson} instance.
 * 
 * @author mchaberski
 */
public class DeserializableCookie implements ClientCookie {

    static final String FIELD_ATTRIBUTES = "attribs";
    static final String FIELD_DOMAIN = "cookieDomain";
    static final String ATTR_DOMAIN = "Domain";
    static final String ATTR_MAX_AGE = "max-age";
    @SuppressWarnings("unused")
    static final String FIELD_HTTP_ONLY = "httpOnly";
    static final String FIELD_LAST_ACCESSED = "lastAccessed";
    static final String FIELD_CREATION_DATE = "creationDate";
    static final String FIELD_EXPIRY_DATE = "cookieExpiryDate";
    private static final int DEFAULT_COOKIE_VERSION = 0;

    @SuppressWarnings("unused") // for gson deserialization
    private DeserializableCookie() {
        this(new Builder("", ""));
    }

    private final String name;
    private final String value;
    private final String cookiePath;
    private final String cookieDomain;
    @JsonAdapter(ImmutableStringMapTypeAdapter.class)
    private final ImmutableMap<String, String> attribs;
    @JsonAdapter(DateTolerantInstantTypeAdapter.class)
    private final Instant cookieExpiryDate;
    @JsonAdapter(DateTolerantInstantTypeAdapter.class)
    private final Instant creationDate;
    @JsonAdapter(DateTolerantInstantTypeAdapter.class)
    private final Instant lastAccessed;
    private final Boolean isSecure;
    private final Boolean httpOnly;
    private final Integer cookieVersion;
    private final String cookieComment;

    private DeserializableCookie(Builder builder) {
        name = builder.name;
        attribs = ImmutableMap.copyOf(builder.attribs.entrySet().stream().filter(e -> e.getValue() != null).collect(Collectors.toSet()));
        value = builder.value;
        cookieComment = builder.cookieComment;
        cookieDomain = builder.cookieDomain;
        cookieExpiryDate = builder.cookieExpiryDate;
        cookiePath = builder.cookiePath;
        isSecure = builder.isSecure;
        cookieVersion = builder.cookieVersion;
        creationDate = builder.creationDate;
        lastAccessed = builder.lastAccessed;
        httpOnly = builder.httpOnly;
    }

    public static Builder builder(String name, String value) {
        return new Builder(name, value);
    }

    /**
     * Gets the attribute value that corresponds to the given key case-insensitively.
     * @param name the key
     * @return the attribute value, or null if not present
     */
    @Override
    public String getAttribute(final String name) {
        String value = attribs.get(name);
        if (value != null) {
            return value;
        }
        String casedKey = findAttributeKey(name);
        if (casedKey != null) {
            return attribs.get(casedKey);
        } else {
            return null;
        }
    }

    protected @Nullable String findAttributeKey(final String name) {
        return attribs.keySet().stream().map(String::toLowerCase)
                .filter(name::equalsIgnoreCase).findFirst().orElse(null);
    }

    @Override
    public boolean containsAttribute(String name) {
        return attribs.containsKey(name) || findAttributeKey(name) != null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getComment() {
        return cookieComment;
    }

    @Override
    public String getCommentURL() {
        return null;
    }

    /**
     * @deprecated use {@link #getExpiryInstant()} instead
     */
    @Deprecated
    @Override
    public Date getExpiryDate() {
        return cookieExpiryDate == null ? null : Date.from(cookieExpiryDate);
    }

    public Instant getExpiryInstant() {
        return cookieExpiryDate;
    }

    @Override
    public boolean isPersistent() {
        if (cookieExpiryDate != null) {
            return true;
        }
        return getAttribute(ATTR_MAX_AGE) != null;
    }

    @Override
    public String getDomain() {
        return cookieDomain;
    }

    @Override
    public String getPath() {
        return cookiePath;
    }

    @Override
    public int[] getPorts() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return isSecure != null && isSecure.booleanValue();
    }

    @Override
    public int getVersion() {
        return cookieVersion != null ? cookieVersion.intValue() : DEFAULT_COOKIE_VERSION;
    }

    /**
     * Returns true if this cookie has expired.
     * @param now Current time
     *
     * @return {@code true} if the cookie has expired.
     */
    @Override
    public boolean isExpired(final Date now) {
        requireNonNull(now, "Date");
        return isExpired(now.toInstant());
    }

    public boolean isExpired(final Instant now) {
        requireNonNull(now, "instant");
        Instant cookieExpiryDate_ = this.cookieExpiryDate;
        return cookieExpiryDate_ != null && (cookieExpiryDate_.isBefore(now) || cookieExpiryDate.equals(now));
    }

    /**
     * @deprecated use {@link #getCreationInstant()} instead
     */
    @Deprecated
    public Date getCreationDate() {
        return creationDate == null ? null : Date.from(creationDate);
    }

    public Instant getCreationInstant() {
        return creationDate;
    }

    /**
     * @deprecated use {@link #getLastAccessedInstant()} instead
     */
    @Deprecated
    public Date getLastAccessed() {
        return lastAccessed == null ? null : Date.from(lastAccessed);
    }

    public Instant getLastAccessedInstant() {
        return lastAccessed;
    }

    public String getDomainAttribute() {
        return getAttribute(ATTR_DOMAIN);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeserializableCookie)) return false;
        DeserializableCookie that = (DeserializableCookie) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(value, that.value) &&
                Objects.equals(attribs, that.attribs) &&
                Objects.equals(cookieComment, that.cookieComment) &&
                Objects.equals(cookieDomain, that.cookieDomain) &&
                Objects.equals(cookieExpiryDate, that.cookieExpiryDate) &&
                Objects.equals(cookiePath, that.cookiePath) &&
                Objects.equals(isSecure, that.isSecure) &&
                Objects.equals(cookieVersion, that.cookieVersion) &&
                Objects.equals(creationDate, that.creationDate) &&
                Objects.equals(lastAccessed, that.lastAccessed) &&
                Objects.equals(httpOnly, that.httpOnly);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name, value, attribs, cookieComment, cookieDomain, cookieExpiryDate, cookiePath, isSecure, cookieVersion, creationDate, lastAccessed, httpOnly);
    }

    protected static boolean equals(Date a, Date b) {
        if (a == b) {
            return true;
        }
        if ((a == null) != (b == null)) {
            return false;
        }
        return DateUtils.truncatedEquals(a, b, Calendar.SECOND);
    }

    public ImmutableMap<String, String> copyAttributes() {
        return ImmutableMap.copyOf(attribs);
    }

    public boolean isHttpOnly() {
        return httpOnly !=  null && httpOnly.booleanValue();
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper h = MoreObjects.toStringHelper(this);
        h.add("name", name);
        h.add("path", cookiePath);
        if (value != null) h.add("value", StringUtils.abbreviate(value, 36));
        if (attribs != null) h.add("attribs", attribs);
        if (cookieComment != null) h.add("cookieComment", cookieComment);
        if (cookieDomain != null) h.add("cookieDomain", cookieDomain);
        if (cookieExpiryDate != null) h.add("cookieExpiryDate", cookieExpiryDate);
        if (isSecure != null) h.add("isSecure", isSecure);
        if (cookieVersion != null) h.add("cookieVersion", cookieVersion);
        if (creationDate != null) h.add("creationDate", creationDate);
        if (lastAccessed != null) h.add("lastAccessed", lastAccessed);
        if (httpOnly != null) h.add("httpOnly", httpOnly);
        return h.toString();
    }

    private static final BigInteger LONG_MIN_BIGINT = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger LONG_MAX_BIGINT = BigInteger.valueOf(Long.MAX_VALUE);

    private static long clamp(BigInteger value, BigInteger min, BigInteger max) {
        if (value.compareTo(min) < 0) {
            return min.longValueExact();
        }
        if (value.compareTo(max) > 0) {
            return max.longValueExact();
        }
        return value.longValueExact();
    }

    @Nullable
    public Instant getBestExpiry() {
        String maxAgeStr = getAttribute(ATTR_MAX_AGE);
        Instant creationDate = getCreationInstant();
        if (maxAgeStr != null && creationDate != null) {
            BigInteger maxAgeSeconds = null;
            try {
                 maxAgeSeconds = new BigInteger(maxAgeStr);
            } catch (RuntimeException ignore) {
            }
            if (maxAgeSeconds != null) {
                long maxAgeSecondsLong = clamp(maxAgeSeconds, LONG_MIN_BIGINT, LONG_MAX_BIGINT);
                long maxAgeMillis = LongMath.saturatedMultiply(maxAgeSecondsLong, 1000);
                long creationEpochMillis = creationDate.toEpochMilli();
                long expiryEpochMillis = LongMath.saturatedAdd(creationEpochMillis, maxAgeMillis);
                Instant later = Instant.ofEpochMilli(expiryEpochMillis);
                return later;
            }
        }
        return getExpiryInstant();
    }

    @Nullable
    public String getBestDomainProperty() {
        String domainAttr = getDomainAttribute();
        if (domainAttr != null) {
            return domainAttr;
        }
        return getDomain();
    }


    @SuppressWarnings({"BooleanParameter", "UnusedReturnValue"})
    public static final class Builder implements SetCookie {
        private final String name;
        private final Map<String, String> attribs = new LinkedHashMap<>();
        private final String value;
        private String cookieComment;
        private String cookieDomain;
        private Instant cookieExpiryDate;
        private String cookiePath;
        private Boolean isSecure;
        private Integer cookieVersion;
        private Instant creationDate;
        private Instant lastAccessed;
        private Boolean httpOnly;

        private Builder(String name, String value) {
            this.name = checkNotNull(name);
            this.value = Strings.nullToEmpty(value);
        }

        @SuppressWarnings("unused")
        public Builder attribute(String key, String value) {
            this.attribs.put(key, value);
            return this;
        }

        public Builder attributes(Map<String, String> val) {
            this.attribs.putAll(val);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder comment(String val) {
            cookieComment = val;
            return this;
        }

        public Builder domain(String val) {
            cookieDomain = val;
            return this;
        }

        public Builder expiry(Instant cookieExpiryDate) {
            this.cookieExpiryDate = cookieExpiryDate;
            return this;
        }

        /**
         * @deprecated use {@link #expiry(Instant)} instead
         */
        @Deprecated
        public Builder expiry(Date val) {
            if (val == null) {
                cookieExpiryDate = null;
            } else {
                cookieExpiryDate = val.toInstant();
            }
            return this;
        }

        public Builder path(String val) {
            cookiePath = val;
            return this;
        }

        public Builder secure(boolean val) {
            isSecure = val;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder version(int val) {
            cookieVersion = val;
            return this;
        }

        /**
         * @deprecated use {@link #creationDate(Instant)} instead
         */
        @Deprecated
        public Builder creationDate(Date val) {
            if (val == null) {
                this.creationDate = null;
            } else {
                creationDate = val.toInstant();
            }
            return this;
        }

        public Builder creationDate(Instant val) {
            creationDate = val;
            return this;
        }

        /**
         * @deprecated use {@link #lastAccessed(Instant)} instead
         */
        @Deprecated
        public Builder lastAccessed(Date val) {
            //noinspection RedundantCast
            return lastAccessed((Instant) (val == null ? null : val.toInstant()));
        }

        public Builder lastAccessed(Instant val) {
            lastAccessed = val;
            return this;
        }

        public Builder httpOnly(boolean val) {
            httpOnly = val;
            return this;
        }

        public DeserializableCookie build() {
            return new DeserializableCookie(this);
        }

        @Override
        public void setComment(String cookieComment) {
            this.cookieComment = cookieComment;
        }

        @Override
        public void setDomain(String cookieDomain) {
            this.cookieDomain = cookieDomain;
        }

        @Override
        @Deprecated
        public void setExpiryDate(Date cookieExpiryDate) {
            if (cookieExpiryDate == null) {
                this.cookieExpiryDate = null;
            } else {
                this.cookieExpiryDate = cookieExpiryDate.toInstant();
            }
        }

        @Override
        public void setPath(String cookiePath) {
            this.cookiePath = cookiePath;
        }

        @Override
        public void setSecure(boolean secure) {
            isSecure = secure;
        }

        @Override
        public void setVersion(int cookieVersion) {
            this.cookieVersion = cookieVersion;
        }

        @Override
        public void setValue(String value) {
            throw new UnsupportedOperationException("this is a cookie builder, not a mutable cookie object");
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException("this is a cookie builder, not a mutable cookie object");
        }

        @Override
        public String getValue() {
            throw new UnsupportedOperationException("this is a cookie builder, not a mutable cookie object");
        }

        @Override
        public String getComment() {
            throw new UnsupportedOperationException("this is a cookie builder, not a mutable cookie object");
        }

        @Override
        public String getCommentURL() {
            throw new UnsupportedOperationException("this is a cookie builder, not a mutable cookie object");
        }

        @Override
        public Date getExpiryDate() {
            throw new UnsupportedOperationException("this is a cookie builder, not a mutable cookie object");
        }

        @Override
        public boolean isPersistent() {
            throw new UnsupportedOperationException("this is a cookie builder, not a mutable cookie object");
        }

        @Override
        public String getDomain() {
            throw new UnsupportedOperationException("this is a cookie builder, not a mutable cookie object");
        }

        @Override
        public String getPath() {
            throw new UnsupportedOperationException("this is a cookie builder, not a mutable cookie object");
        }

        @Override
        public int[] getPorts() {
            throw new UnsupportedOperationException("this is a cookie builder, not a mutable cookie object");
        }

        @Override
        public boolean isSecure() {
            throw new UnsupportedOperationException("this is a cookie builder, not a mutable cookie object");
        }

        @Override
        public int getVersion() {
            throw new UnsupportedOperationException("this is a cookie builder, not a mutable cookie object");
        }

        @Override
        public boolean isExpired(Date date) {
            throw new UnsupportedOperationException("this is a cookie builder, not a mutable cookie object");
        }
    }
}
