package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.core.har.Har;

import static com.google.common.base.Preconditions.checkNotNull;

public class HarPlus<T> {

    public final Har har;
    public final T result;

    public HarPlus(Har har, T result) {
        this.har = checkNotNull(har);
        this.result = result;
    }

    public static HarPlus<Void> nothing(Har har) {
        return new HarPlus<>(har, (Void)null);
    }

    @Override
    public String toString() {
        return "HarPlus{" +
                "har=" + har +
                ", result=" + result +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HarPlus<?> harPlus = (HarPlus<?>) o;

        if (!har.equals(harPlus.har)) return false;
        return result != null ? result.equals(harPlus.result) : harPlus.result == null;
    }

    @Override
    public int hashCode() {
        int result1 = har.hashCode();
        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
        return result1;
    }
}
