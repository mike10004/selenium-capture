package com.github.mike10004.seleniumhelp;

import java.util.Map;

public interface ExplodedCookieConverter {

    Map<String, Object> explode(DeserializableCookie cookie);
    DeserializableCookie reassemble(Map<String, Object> explosion);

}

