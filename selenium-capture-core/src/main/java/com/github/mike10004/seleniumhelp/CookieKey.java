package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import javax.annotation.Nullable;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class CookieKey {
    public final String domain;
    public final String name;
    public final String path;

    private CookieKey(String domain, String name, String path) {
        this.domain = requireNonNull(domain);
        this.name = requireNonNull(name);
        this.path = requireNonNull(path);
    }

    public static CookieKey from(@Nullable String domain, @Nullable String name, @Nullable String path) {
        //noinspection ConstantConditions
        return new CookieKey(Strings.nullToEmpty(domain), Strings.nullToEmpty(name), MoreObjects.firstNonNull(path, "/"));
    }

    @Override
    public String toString() {
        return "CookieKey{" +
                "domain='" + domain + '\'' +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CookieKey cookieKey = (CookieKey) o;
        return Objects.equals(domain, cookieKey.domain) &&
                Objects.equals(name, cookieKey.name) &&
                Objects.equals(path, cookieKey.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(domain, name, path);
    }

    public static CookieKey from(DeserializableCookie cookie) {
        return CookieKey.from(cookie.getBestDomainProperty(), cookie.getName(), cookie.getPath());
    }

    public boolean matches(DeserializableCookie cookie) {
        return domain.equalsIgnoreCase(cookie.getBestDomainProperty())
                && name.equals(cookie.getName())
                && path.equals(cookie.getPath());
    }

}
