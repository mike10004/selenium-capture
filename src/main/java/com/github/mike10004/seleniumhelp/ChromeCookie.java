package com.github.mike10004.seleniumhelp;

/**
 * Class that represents a cookie with structure defined by the Chrome Extensions API.
 * See https://developer.chrome.com/extensions/cookies#method-set.
 */
public class ChromeCookie {

    @SuppressWarnings("unused")
    public enum SameSiteStatus { no_restriction, lax, strict }

    public String url;
    public String name;
    public String value;
    public String domain;
    public String path;
    public Boolean secure;
    public Boolean httpOnly;
    public SameSiteStatus sameSite;
    public Boolean session;
    public Double expirationDate;
    public String storeId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChromeCookie)) return false;

        ChromeCookie that = (ChromeCookie) o;

        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        if (domain != null ? !domain.equals(that.domain) : that.domain != null) return false;
        if (path != null ? !path.equals(that.path) : that.path != null) return false;
        if (secure != null ? !secure.equals(that.secure) : that.secure != null) return false;
        if (httpOnly != null ? !httpOnly.equals(that.httpOnly) : that.httpOnly != null) return false;
        if (sameSite != that.sameSite) return false;
        if (session != null ? !session.equals(that.session) : that.session != null) return false;
        if (expirationDate != null ? !expirationDate.equals(that.expirationDate) : that.expirationDate != null)
            return false;
        return storeId != null ? storeId.equals(that.storeId) : that.storeId == null;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (domain != null ? domain.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (secure != null ? secure.hashCode() : 0);
        result = 31 * result + (httpOnly != null ? httpOnly.hashCode() : 0);
        result = 31 * result + (sameSite != null ? sameSite.hashCode() : 0);
        result = 31 * result + (session != null ? session.hashCode() : 0);
        result = 31 * result + (expirationDate != null ? expirationDate.hashCode() : 0);
        result = 31 * result + (storeId != null ? storeId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ChromeCookie{" +
                "url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", domain='" + domain + '\'' +
                ", path='" + path + '\'' +
                ", secure=" + secure +
                ", httpOnly=" + httpOnly +
                ", sameSite=" + sameSite +
                ", session=" + session +
                ", expirationDate=" + expirationDate +
                ", storeId='" + storeId + '\'' +
                '}';
    }
}
