package com.github.mike10004.seleniumhelp;

import com.google.common.base.Function;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImmutableHttpHeaders extends HttpHeaders {

    private final HttpHeaders inner;
    private final ImmutableList<Entry<String, String>> entries;
    private final ImmutableSet<String> names;

    public ImmutableHttpHeaders(HttpHeaders inner) {
        this.inner = inner;
        entries = ImmutableList.copyOf(inner.entries().stream().map(input -> {
            checkNotNull(input);
            if (input instanceof SimpleImmutableEntry) {
                return (SimpleImmutableEntry<String, String>) input;
            } else {
                return new SimpleImmutableEntry<>(input.getKey(), input.getValue());
            }
        }).collect(Collectors.toList()));
        names = ImmutableSet.copyOf(inner.names());
    }

    public static java.util.function.Supplier<HttpHeaders> asSupplier(final HttpMessage message) {
        return () -> new ImmutableHttpHeaders(message.headers());
    }

    public static java.util.function.Supplier<HttpHeaders> memoize(final HttpMessage message) {
        return Suppliers.memoize(asSupplier(message)::get)::get;
    }

    @Override
    public String get(String name) {
        return inner.get(name);
    }

    @Override
    public String get(CharSequence name) {
        return inner.get(name);
    }

    @Override
    public List<String> getAll(String name) {
        return inner.getAll(name);
    }

    @Override
    public List<String> getAll(CharSequence name) {
        return inner.getAll(name);
    }

    @Override
    public List<Entry<String, String>> entries() {
        return entries;
    }

    @Override
    public boolean contains(String name) {
        return inner.contains(name);
    }

    @Override
    public boolean contains(CharSequence name) {
        return inner.contains(name);
    }

    @Override
    public boolean isEmpty() {
        return inner.isEmpty();
    }

    @Override
    public Set<String> names() {
        return names;
    }

    @Override
    public HttpHeaders add(String name, Object value) {
        return inner.add(name, value);
    }

    @Override
    public HttpHeaders add(CharSequence name, Object value) {
        return inner.add(name, value);
    }

    @Override
    public HttpHeaders add(String name, Iterable<?> values) {
        return inner.add(name, values);
    }

    @Override
    public HttpHeaders add(CharSequence name, Iterable<?> values) {
        return inner.add(name, values);
    }

    @Override
    public HttpHeaders add(HttpHeaders headers) {
        return inner.add(headers);
    }

    @Override
    public HttpHeaders set(String name, Object value) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public HttpHeaders set(CharSequence name, Object value) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public HttpHeaders set(String name, Iterable<?> values) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public HttpHeaders set(CharSequence name, Iterable<?> values) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public HttpHeaders set(HttpHeaders headers) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public HttpHeaders remove(String name) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public HttpHeaders remove(CharSequence name) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public HttpHeaders clear() {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public boolean contains(String name, String value, boolean ignoreCaseValue) {
        return inner.contains(name, value, ignoreCaseValue);
    }

    @Override
    public boolean containsValue(CharSequence name, CharSequence value, boolean ignoreCase) {
        return inner.containsValue(name, value, ignoreCase);
    }

    @Override
    public boolean contains(CharSequence name, CharSequence value, boolean ignoreCaseValue) {
        return inner.contains(name, value, ignoreCaseValue);
    }

    @SuppressWarnings({"DeprecatedIsStillUsed"})
    @Override
    @Deprecated
    public Iterator<Entry<String, String>> iterator() {
        return new IteratorNotSupportingRemove<>(inner.iterator());
    }

    private static class IteratorNotSupportingRemove<E> implements Iterator<E> {

        private final Iterator<E> inner;

        public IteratorNotSupportingRemove(Iterator<E> inner) {
            this.inner = checkNotNull(inner);
        }

        @Override
        public boolean hasNext() {
            return inner.hasNext();
        }

        @Override
        public E next() {
            return inner.next();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            inner.forEachRemaining(action);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove is not supported");
        }
    }

    @Override
    public String toString() {
        return inner.toString();
    }

    @SuppressWarnings("unchecked") // we're casting strings to charsequences
    @Override
    public Iterator<Entry<CharSequence, CharSequence>> iteratorCharSequence() {
        //noinspection deprecation
        return Iterators.transform(iterator(), new Function<Entry, Entry<CharSequence, CharSequence>>() {
            @Override
            public Entry<CharSequence, CharSequence> apply(@SuppressWarnings("NullableProblems") Entry input) {
                return (Entry<CharSequence, CharSequence>) input;
            }
        });
    }

    @Override
    public Integer getInt(CharSequence name) {
        return inner.getInt(name);
    }

    @Override
    public int getInt(CharSequence name, int defaultValue) {
        return inner.getInt(name, defaultValue);
    }

    @Override
    public Short getShort(CharSequence name) {
        return inner.getShort(name);
    }

    @Override
    public short getShort(CharSequence name, short defaultValue) {
        return inner.getShort(name, defaultValue);
    }

    @Override
    public Long getTimeMillis(CharSequence name) {
        return inner.getTimeMillis(name);
    }

    @Override
    public long getTimeMillis(CharSequence name, long defaultValue) {
        return inner.getTimeMillis(name, defaultValue);
    }

    @Override
    public int size() {
        return inner.size();
    }

    @Deprecated
    @Override
    public HttpHeaders addInt(CharSequence name, int value) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public HttpHeaders addShort(CharSequence name, short value) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public HttpHeaders setInt(CharSequence name, int value) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public HttpHeaders setShort(CharSequence name, short value) {
        throw new UnsupportedOperationException("immutable");
    }
}