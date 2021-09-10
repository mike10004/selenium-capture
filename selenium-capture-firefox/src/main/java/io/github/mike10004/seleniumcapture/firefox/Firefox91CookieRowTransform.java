package io.github.mike10004.seleniumcapture.firefox;

public class Firefox91CookieRowTransform extends FirefoxCookieRowTransformBase {

    public Firefox91CookieRowTransform() {
        super(Firefox91CookieImporter.getImportInfo().columnNames(), new Firefox91CookieValueGetter());
    }

}

