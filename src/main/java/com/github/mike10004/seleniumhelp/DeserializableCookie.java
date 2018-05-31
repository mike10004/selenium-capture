/*
 * (c) 2016 ${COPYRIGHTER}
 */
package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.SetCookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
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
    @SuppressWarnings("unused")
    static final String FIELD_HTTP_ONLY = "httpOnly";
    static final String FIELD_LAST_ACCESSED = "lastAccessed";
    static final String FIELD_CREATION_DATE = "creationDate";
    static final String FIELD_EXPIRY_DATE = "cookieExpiryDate";

    @SuppressWarnings("unused") // for gson deserialization
    private DeserializableCookie() {
        this(new Builder("", ""));
    }

    private final String name;
    private final String value;
    @JsonAdapter(ImmutableStringMapTypeAdapter.class)
    private final ImmutableMap<String, String> attribs;
    private final String cookieComment;
    private final String cookieDomain;
    private final Date cookieExpiryDate;
    private final String cookiePath;
    private final boolean isSecure;
    private final int cookieVersion;
    private final Date creationDate;
    private final Date lastAccessed;
    private final boolean httpOnly;

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

    @Override
    public Date getExpiryDate() {
        return cookieExpiryDate;
    }

    @Override
    public boolean isPersistent() {
        return null != cookieExpiryDate;
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
        return isSecure;
    }

    @Override
    public int getVersion() {
        return cookieVersion;
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
        Date cookieExpiryDate_ = this.cookieExpiryDate;
        return cookieExpiryDate_ != null && (cookieExpiryDate_.before(now) || cookieExpiryDate.equals(now));
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getLastAccessed() {
        return lastAccessed;
    }

    public String getDomainAttribute() {
        return getAttribute("Domain");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeserializableCookie that = (DeserializableCookie) o;

        if (isSecure != that.isSecure) return false;
        if (httpOnly != that.httpOnly) return false;
        if (cookieVersion != that.cookieVersion) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (attribs != null ? !attribs.equals(that.attribs) : that.attribs != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        if (cookieComment != null ? !cookieComment.equals(that.cookieComment) : that.cookieComment != null)
            return false;
        if (cookieDomain != null ? !cookieDomain.equals(that.cookieDomain) : that.cookieDomain != null) return false;
        if (cookieExpiryDate != null ? !equals(cookieExpiryDate, that.cookieExpiryDate) : that.cookieExpiryDate != null)
            return false;
        if (cookiePath != null ? !cookiePath.equals(that.cookiePath) : that.cookiePath != null) return false;
        if (creationDate != null ? !equals(creationDate, that.creationDate) : that.creationDate != null) return false;
        return lastAccessed != null ? lastAccessed.equals(that.lastAccessed) : that.lastAccessed == null;

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

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (attribs != null ? attribs.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (cookieComment != null ? cookieComment.hashCode() : 0);
        result = 31 * result + (cookieDomain != null ? cookieDomain.hashCode() : 0);
        result = 31 * result + (cookieExpiryDate != null ? cookieExpiryDate.hashCode() : 0);
        result = 31 * result + (cookiePath != null ? cookiePath.hashCode() : 0);
        result = 31 * result + (isSecure ? 1 : 0);
        result = 31 * result + (httpOnly ? 1 : 0);
        result = 31 * result + cookieVersion;
        result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
        result = 31 * result + (lastAccessed != null ? lastAccessed.hashCode() : 0);
        return result;
    }

    public ImmutableMap<String, String> copyAttributes() {
        return ImmutableMap.copyOf(attribs);
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper h = MoreObjects.toStringHelper(this);
        h.add("name", name);
        if (value != null) h.add("value", StringUtils.abbreviate(value, 36));
        if (attribs != null) h.add("attribs", attribs);
        if (cookieComment != null) h.add("cookieComment", cookieComment);
        if (cookieDomain != null) h.add("cookieDomain", cookieDomain);
        if (cookieExpiryDate != null) h.add("cookieExpiryDate", cookieExpiryDate);
        h.add("cookiePath", cookiePath);
        h.add("isSecure", isSecure);
        h.add("cookieVersion", cookieVersion);
        if (creationDate != null) h.add("creationDate", creationDate);
        if (lastAccessed != null) h.add("lastAccessed", lastAccessed);
        h.add("httpOnly", httpOnly);
        return h.toString();
    }

    public String getBestDomainProperty() {
        String domainAttr = getDomainAttribute();
        if (domainAttr != null) {
            return domainAttr;
        }
        return getDomain();
    }


    @SuppressWarnings("BooleanParameter")
    public static final class Builder implements SetCookie {
        private final String name;
        private final Map<String, String> attribs = new LinkedHashMap<>();
        private final String value;
        private String cookieComment;
        private String cookieDomain;
        private Date cookieExpiryDate;
        private String cookiePath;
        private boolean isSecure;
        private int cookieVersion;
        private Date creationDate;
        private Date lastAccessed;
        private boolean httpOnly;

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

        public Builder expiry(Date val) {
            cookieExpiryDate = val;
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

        public Builder creationDate(Date val) {
            creationDate = val;
            return this;
        }

        public Builder lastAccessed(Date val) {
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
        public void setExpiryDate(Date cookieExpiryDate) {
            this.cookieExpiryDate = cookieExpiryDate;
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
