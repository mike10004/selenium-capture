package io.github.mike10004.seleniumcapture.firefox;

import io.github.mike10004.seleniumcapture.DeserializableCookie;

import java.util.Map;

public interface FirefoxCookieReassembler {
    DeserializableCookie reassemble(Map<String, Object> explosion);
}
