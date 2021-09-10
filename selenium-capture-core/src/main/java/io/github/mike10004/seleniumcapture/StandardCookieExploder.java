package io.github.mike10004.seleniumcapture;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class StandardCookieExploder implements ExplodedCookieConverter {

    private static final Logger log = LoggerFactory.getLogger(StandardCookieExploder.class);

    private static final Gson gson = new GsonBuilder().create();

    private static final ImmutableMap<String, String> IMPLIED_MISSING = ImmutableMap.<String, String>builder()
            .put("cookieVersion", "0")
            .put("cookieExpiryDate", "0")
            .put("lastAccessed", "0")
            .put("creationDate", "0")
            .put("attribs", "{}")
            .put("httpOnly", "false")
            .put("isSecure", "false")
            .build();

    public StandardCookieExploder() {
    }


    @Override
    public Map<String, Object> explode(DeserializableCookie cookie) {
        JsonObject json = gson.toJsonTree(cookie).getAsJsonObject();
        removeDefaults(json);
        Map<String, Object> map = gson.fromJson(json, getDeserializationTypeToken().getType());
        dateify(cookie, map);
        log.trace("cookie serialized as object with {} fields, converted to map with {} fields", json.size(), map.size());
        return map;
    }

    private static TypeToken<LinkedTreeMap<String, Object>> getDeserializationTypeToken() {
        return new TypeToken<LinkedTreeMap<String, Object>>() {};
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
        for (String name : IMPLIED_MISSING.keySet()) {
            String jsonEncodingThatImpliesAbsence = IMPLIED_MISSING.get(name);
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
