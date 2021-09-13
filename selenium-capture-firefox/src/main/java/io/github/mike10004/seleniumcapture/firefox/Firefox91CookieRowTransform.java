package io.github.mike10004.seleniumcapture.firefox;

import io.github.mike10004.seleniumcapture.StandardCookieExploder;

public class Firefox91CookieRowTransform extends FirefoxCookieRowTransformBase {

    public Firefox91CookieRowTransform() {
        super(new StandardCookieExploder(), Firefox91CookieImporter.getImportInfo().columnNames(), new Firefox91CookieValueGetter());
    }

}

