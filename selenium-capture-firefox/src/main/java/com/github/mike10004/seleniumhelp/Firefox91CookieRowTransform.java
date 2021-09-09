package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InternetDomainName;

import java.util.Map;
import java.util.function.Function;

public class Firefox91CookieRowTransform extends FirefoxCookieRowTransformBase {

    public Firefox91CookieRowTransform() {
        super(Firefox91CookieImporter.getImportInfo().columnNames(), new Firefox91CookieValueGetter());
    }

}

