package com.github.mike10004.seleniumhelp;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;

import java.util.Date;
import java.util.Map;

public class ExplodedCookieConverter extends Converter<Map<String, Object>, DeserializableCookie> {

    private static final Gson gson = new GsonBuilder().create();
    private static final ImmutableMap<String, String> impliedMissing = ImmutableMap.<String, String>builder()
            .put("cookieVersion", "0")
            .put("cookieExpiryDate", "0")
            .put("lastAccessed", "0")
            .put("creationDate", "0")
            .put("attribs", "{}")
            .put("httpOnly", "false")
            .build();

    @Override
    protected DeserializableCookie doForward(Map<String, Object> intermediateRep) {
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

    private void maybeDateify(Map<String, Object> map, String key, Date value) {
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
        maybeDateify(map, "lastAccessed", cookie.getLastAccessed());
        maybeDateify(map, "creationDate", cookie.getCreationDate());
        maybeDateify(map, "cookieExpiryDate", cookie.getExpiryDate());
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
