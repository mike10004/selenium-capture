package com.github.mike10004.seleniumhelp;

import com.browserup.harreader.model.Har;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that represents the result of a traffic capture session.
 * @param <T>
 * @see TrafficCollector#collect(TrafficGenerator)
 */
public class HarPlus<T> {

    /**
     * HAR object containing HTTP interactions of a web browsing session.
     */
    public final Har har;

    /**
     * Arbitrary result object obtained during a web browsing session.
     */
    public final T result;

    /**
     * Constructs an instance.
     * @param har the HAR file
     * @param result a result object
     */
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
