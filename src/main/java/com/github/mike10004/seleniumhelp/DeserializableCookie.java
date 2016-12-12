/*
 * (c) 2016 ${COPYRIGHTER}
 */
package com.github.mike10004.seleniumhelp;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.SetCookie;
import org.apache.http.util.Args;
import org.openqa.selenium.Cookie;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Class that represents a cookie that's easy to deserialize from json.
 * The purpose of this class is to substitute for 
 * {@link org.apache.http.impl.cookie.BasicClientCookie}, which does not have
 * a no-args constructor, during deserialization by a 
 * {@link com.google.gson.Gson} instance.
 * 
 * @author mchaberski
 */
public class DeserializableCookie implements ClientCookie, SetCookie {

    static final String FIELD_ATTRIBUTES = "attribs";
    static final String FIELD_DOMAIN = "cookieDomain";
    static final String FIELD_HTTP_ONLY = "httpOnly";
    static final String FIELD_LAST_ACCESSED = "lastAccessed";
    static final String FIELD_CREATION_DATE = "creationDate";
    static final String FIELD_EXPIRY_DATE = "cookieExpiryDate";

    public DeserializableCookie() {
        super();
        this.attribs = new HashMap<>();
    }

    private String name;
    private final Map<String, String> attribs;
    private String value;
    private String cookieComment;
    private String cookieDomain;
    private Date cookieExpiryDate;
    private String cookiePath;
    private boolean isSecure;
    private int cookieVersion;
    private Date creationDate;
    private Date lastAccessed;
    private boolean httpOnly;

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

    public String setAttribute(String name, String value) {
        return attribs.put(name.toLowerCase(java.util.Locale.ROOT), value);
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
        Args.notNull(now, "Date");
        Date cookieExpiryDate_ = this.cookieExpiryDate;
        return cookieExpiryDate_ != null && (cookieExpiryDate_.before(now) || cookieExpiryDate.equals(now));
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getLastAccessed() {
        return lastAccessed;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setComment(String cookieComment) {
        this.cookieComment = cookieComment;
    }

    public void setDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

    public void setExpiryDate(Date cookieExpiryDate) {
        this.cookieExpiryDate = cookieExpiryDate;
    }

    public void setPath(String cookiePath) {
        this.cookiePath = cookiePath;
    }

    public void setSecure(boolean secure) {
        isSecure = secure;
    }

    public void setVersion(int cookieVersion) {
        this.cookieVersion = cookieVersion;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setLastAccessed(Date lastAccessed) {
        this.lastAccessed = lastAccessed;
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
        return org.apache.commons.lang3.time.DateUtils.truncatedEquals(a, b, Calendar.SECOND);
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

    public void replaceAttributes(Map<String, String> attribs) {
        this.attribs.clear();
        this.attribs.putAll(attribs);
    }

    public ImmutableMap<String, String> copyAttributes() {
        return ImmutableMap.copyOf(attribs);
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    @Override
    public String toString() {
        return "DeserializableCookie{" +
                "name='" + name + '\'' +
                ", attribs=" + attribs +
                ", value='" + value + '\'' +
                ", cookieComment='" + cookieComment + '\'' +
                ", cookieDomain='" + cookieDomain + '\'' +
                ", cookieExpiryDate=" + cookieExpiryDate +
                ", cookiePath='" + cookiePath + '\'' +
                ", isSecure=" + isSecure +
                ", cookieVersion=" + cookieVersion +
                ", creationDate=" + creationDate +
                ", lastAccessed=" + lastAccessed +
                ", httpOnly=" + httpOnly +
                '}';
    }

}
