package com.github.mike10004.seleniumhelp;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class Firefox68CookieReassembler implements FirefoxCookieReassembler {

    private static final Logger log = LoggerFactory.getLogger(Firefox68CookieReassembler.class);

    private static final Gson gson = new GsonBuilder().create();

    @Override
    public DeserializableCookie reassemble(Map<String, Object> intermediateRep) {
        intermediateRep = ImmutableMap.copyOf(Maps.transformEntries(intermediateRep, TYPE_ANNOTATION_RESPECTING_TRANSFORM));
        JsonElement json = gson.toJsonTree(intermediateRep);
        DeserializableCookie cookie = gson.fromJson(json, DeserializableCookie.class);
        return cookie;
    }

    private static final Maps.EntryTransformer<String, Object, Object> TYPE_ANNOTATION_RESPECTING_TRANSFORM = new Maps.EntryTransformer<String, Object, Object>() {

        private final LoadingCache<Class<?>, Optional<?>> cache = CacheBuilder.newBuilder()
                .maximumSize(1024)
                .build(new CacheLoader<Class<?>, Optional<?>>() {
                    @Override
                    public Optional<?> load(@SuppressWarnings("NullableProblems") Class<?> key) {
                        try {
                            return Optional.of(key.getDeclaredConstructor().newInstance());
                        } catch (RuntimeException | InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException ignore) {
                            return Optional.empty();
                        }
                    }
                });

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Nullable
        private JsonElement serializeWithAdapter(Object adapterPoly, @SuppressWarnings("unused") String key, Object value) {
            if (adapterPoly instanceof TypeAdapter) {
                return ((TypeAdapter) adapterPoly).toJsonTree(value);
            }
            if (adapterPoly instanceof TypeAdapterFactory) {
                TypeAdapter<?> adapter = ((TypeAdapterFactory) adapterPoly).create(gson, com.google.gson.reflect.TypeToken.get(value.getClass()));
                if (adapter != null) {
                    return serializeWithAdapter(adapter, key, value);
                }
            }
            return null;
        }

        @Nullable
        private JsonElement serializeValue(String key, Object value) {
            try {
                Field field = DeserializableCookie.class.getDeclaredField(key);
                JsonAdapter adapterAnno = field.getAnnotation(JsonAdapter.class);
                if (adapterAnno != null) {
                    Class<?> typeAdapterClass = adapterAnno.value();
                    Optional<?> instanceOpt;
                    try {
                        instanceOpt = cache.get(typeAdapterClass);
                    } catch (ExecutionException ignore) {
                        instanceOpt = Optional.empty();
                    }
                    if (instanceOpt.isPresent()) {
                        return serializeWithAdapter(instanceOpt.get(), key, value);
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                log.debug("failed to serialize reflectively", e);
            }
            return null;
        }

        @Override
        public Object transformEntry(String key, Object value) {
            JsonElement serialized = serializeValue(key, value);
            if (serialized != null) {
                value = serialized;
            }
            return value;
        }
    };

}
