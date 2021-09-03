package com.github.mike10004.seleniumhelp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

public class Firefox91CookieImporter implements FirefoxCookieImporter {

    public Firefox91CookieImporter(Sqlite3Runner sqliteRunner, Sqlite3GenericImporter genericImporter) {

    }

    @Override
    public void importCookies(Iterable<DeserializableCookie> cookies, File sqliteDbFile, Path scratchDir) throws SQLException, IOException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public String getEmptyDbResourcePath() {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
