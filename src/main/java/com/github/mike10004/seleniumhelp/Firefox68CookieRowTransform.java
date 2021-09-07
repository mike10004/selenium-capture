package com.github.mike10004.seleniumhelp;

public class Firefox68CookieRowTransform extends FirefoxCookieRowTransformBase {

    public Firefox68CookieRowTransform() {
        super(Firefox68CookieImporter.getImportInfo().columnNames(), new Firefox68CookieValueGetter());
    }

}

