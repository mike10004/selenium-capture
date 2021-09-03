package com.github.mike10004.seleniumhelp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

public interface FirefoxCookieImporter {
    void importCookies(Iterable<DeserializableCookie> cookies, File sqliteDbFile, Path scratchDir) throws SQLException, IOException;

    String getEmptyDbResourcePath();
}
