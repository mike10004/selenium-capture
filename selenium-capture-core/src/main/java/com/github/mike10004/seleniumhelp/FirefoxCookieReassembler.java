package com.github.mike10004.seleniumhelp;

import java.util.Map;

public interface FirefoxCookieReassembler {
    DeserializableCookie reassemble(Map<String, Object> explosion);
}
