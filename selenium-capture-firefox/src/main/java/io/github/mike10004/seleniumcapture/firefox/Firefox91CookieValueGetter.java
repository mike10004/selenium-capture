package io.github.mike10004.seleniumcapture.firefox;

import io.github.mike10004.seleniumcapture.DeserializableCookie;
import io.github.mike10004.seleniumcapture.MapUtils;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Function;

public class Firefox91CookieValueGetter implements FirefoxCookieValueGetter {

    public Firefox91CookieValueGetter() {
    }

    @Override
    public Object getValueBySqlColumnName(Map<String, Object> explodedCookie, String sqlColumnName) {
        Function<Map<String, Object>, Object> fn = VALUE_GETTER_MAP.get(sqlColumnName);
        if (fn != null) {
            return fn.apply(explodedCookie);
        }
        return explodedCookie.get(sqlColumnName);
    }

    @Override
    public void supplementSqlFields(Map<String, Object> explodedCookie, Map<String, String> sqlRow) {
        // noop
    }

    private static final ImmutableMap<String, Function<Map<String, Object>, Object>> VALUE_GETTER_MAP = ImmutableBiMap.<String, Function<Map<String, Object>, Object>>builder()
            .put("path", FirefoxCookieRowTransformBase.valueByKey(DeserializableCookie.FIELD_PATH))
            .put("expiry", FirefoxCookieRowTransformBase.valueByKey(DeserializableCookie.FIELD_EXPIRY_DATE))
            .put("creationTime", FirefoxCookieRowTransformBase.valueByKey(DeserializableCookie.FIELD_CREATION_DATE))
            .put("originAttributes", FirefoxCookieRowTransformBase.valueByKey(DeserializableCookie.FIELD_ATTRIBUTES))
            .put("isHttpOnly", FirefoxCookieRowTransformBase.valueByKey(DeserializableCookie.FIELD_HTTP_ONLY))
            .put("host", Firefox91CookieValueGetter::getHost)
            // TODO we have to pin down how firefox transforms SameSite=(None|Strict|Lax) into an integer value
            .put("sameSite", Firefox91CookieValueGetter::getSameSite)
            // TODO figure out what firefox means by "rawSameSite"
            .put("rawSameSite", Firefox91CookieValueGetter::getSameSite)
            .build();

    private static Object getHost(Map<String, Object> explodedCookie) {
        String domain = getAttribute(explodedCookie, "domain");
        if (!Strings.isNullOrEmpty(domain)) {
            return domain;
        }
        domain = (String) explodedCookie.get(DeserializableCookie.FIELD_DOMAIN);
        return domain;
    }

    private static String getAttribute(Map<String, Object> explodedCookie, String key) {
        @SuppressWarnings("unchecked")
        @Nullable Map<String, String> attrs = (Map<String, String>) explodedCookie.get(DeserializableCookie.FIELD_ATTRIBUTES);
        String value = MapUtils.getValueByCaseInsensitiveKey(attrs, key);
        return value;
    }

    @Nullable
    private static Integer getSameSite(Map<String, Object> explodedCookie) {
        String sameSiteAttrValue = getAttribute(explodedCookie, "SameSite");
        if (sameSiteAttrValue == null) {
            return null;
        }
        Integer value = MapUtils.getValueByCaseInsensitiveKey(SAME_SITE_MAP, sameSiteAttrValue);
        if (value != null) {
            return value;
        }
        return 0;
    }

    private static final ImmutableMap<String, Integer> SAME_SITE_MAP = ImmutableMap.<String, Integer>builder()
            .put("None", 0)
            .put("Strict", 1)
            .put("Lax", 2)
            .build();

}
