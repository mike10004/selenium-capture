package io.github.mike10004.seleniumcapture.firefox;

import com.github.mike10004.seleniumhelp.DeserializableCookie;

import java.util.Map;

public interface FirefoxCookieReassembler {
    DeserializableCookie reassemble(Map<String, Object> explosion);
}
