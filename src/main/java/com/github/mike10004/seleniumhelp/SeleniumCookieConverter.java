package com.github.mike10004.seleniumhelp;

import com.google.common.base.Converter;

/**
 * Converter that handles conversion of Selenium cookies to this library's cookie format.
 * Converts objects of class {@link org.openqa.selenium.Cookie org.openqa.selenium.Cookie} to and from class
 * {@link DeserializableCookie}.
 * @see DeserializableCookie
 */
public class SeleniumCookieConverter extends Converter<org.openqa.selenium.Cookie, DeserializableCookie> {

    @Override
    protected DeserializableCookie doForward(org.openqa.selenium.Cookie cookie) {
        DeserializableCookie d = DeserializableCookie.builder(cookie.getName(), cookie.getValue())
        .domain(cookie.getDomain())
        .expiry(cookie.getExpiry())
        .path(cookie.getPath())
        .secure(cookie.isSecure())
        .httpOnly(cookie.isHttpOnly()).build();
        return d;
    }

    @Override
    protected org.openqa.selenium.Cookie doBackward(DeserializableCookie d) {
        org.openqa.selenium.Cookie c = new org.openqa.selenium.Cookie(d.getName(), d.getValue(), d.getDomain(), d.getPath(), d.getExpiryDate(), d.isSecure(), d.isHttpOnly());
        return c;
    }
}
