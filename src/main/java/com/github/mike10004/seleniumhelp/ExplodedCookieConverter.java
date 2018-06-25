package com.github.mike10004.seleniumhelp;

import com.google.common.base.Converter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.internal.LinkedTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class ExplodedCookieConverter extends Converter<Map<String, Object>, DeserializableCookie> {

    private static final Logger log = LoggerFactory.getLogger(ExplodedCookieConverter.class);

    private static final Gson gson = new GsonBuilder().create();
    private static final ImmutableMap<String, String> impliedMissing = ImmutableMap.<String, String>builder()
            .put("cookieVersion", "0")
            .put("cookieExpiryDate", "0")
            .put("lastAccessed", "0")
            .put("creationDate", "0")
            .put("attribs", "{}")
            .put("httpOnly", "false")
            .build();

    private static final Maps.EntryTransformer<String, Object, Object> TYPE_ANNOTATION_RESPECTING_TRANSFORM = new Maps.EntryTransformer<String, Object, Object>() {

        private LoadingCache<Class<?>, Optional<?>> cache = CacheBuilder.newBuilder()
            .maximumSize(1024)
            .build(new CacheLoader<Class<?>, Optional<?>>() {
                @Override
                public Optional<?> load(Class<?> key) {
                    try {
                        return Optional.of(key.newInstance());
                    } catch (RuntimeException | InstantiationException | IllegalAccessException ignore) {
                        return Optional.empty();
                    }
                }
            });

        @Nullable
        protected JsonElement serializeWithAdapter(Object adapterPoly, String key, Object value) {
            if (TypeAdapter.class.isInstance(adapterPoly)) {
                return ((TypeAdapter)adapterPoly).toJsonTree(value);
            }
            if (TypeAdapterFactory.class.isInstance(adapterPoly)) {
                TypeAdapter<?> adapter = ((TypeAdapterFactory)adapterPoly).create(gson, com.google.gson.reflect.TypeToken.get(value.getClass()));
                if (adapter != null) {
                    return serializeWithAdapter(adapter, key, value);
                }
            }
            return null;
        }

        @Nullable
        protected JsonElement serializeValue(String key, Object value) {
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

    @Override
    protected DeserializableCookie doForward(Map<String, Object> intermediateRep) {
        intermediateRep = ImmutableMap.copyOf(Maps.transformEntries(intermediateRep, TYPE_ANNOTATION_RESPECTING_TRANSFORM));
        JsonElement json = gson.toJsonTree(intermediateRep);
        DeserializableCookie cookie = gson.fromJson(json, DeserializableCookie.class);
        return cookie;
    }

    @Override
    protected Map<String, Object> doBackward(DeserializableCookie cookie) {
        JsonObject json = gson.toJsonTree(cookie).getAsJsonObject();
        removeDefaults(json);
        Map<String, Object> map = gson.fromJson(json, new TypeToken<LinkedTreeMap<String, Object>>(){}.getType());
        dateify(cookie, map);
        return map;
    }

    private void maybeDateify(Map<String, Object> map, String key, Instant value) {
        if (value == null) {
            map.remove(key);
            return;
        }
        if (!map.containsKey(key)) {
            return;
        }
        map.put(key, value);
    }

    private void dateify(DeserializableCookie cookie, Map<String, Object> map) {
        maybeDateify(map, "lastAccessed", cookie.getLastAccessedInstant());
        maybeDateify(map, "creationDate", cookie.getCreationInstant());
        maybeDateify(map, "cookieExpiryDate", cookie.getExpiryInstant());
    }

    private void removeDefaults(JsonObject object) {
        for (String name : impliedMissing.keySet()) {
            String jsonEncodingThatImpliesAbsence = impliedMissing.get(name);
            JsonElement element = object.get(name);
            if (element != null) {
                String jsonEncodingOfField = element.toString();
                if (jsonEncodingThatImpliesAbsence.equals(jsonEncodingOfField)) {
                    object.remove(name);
                }
            }
        }
    }
}
