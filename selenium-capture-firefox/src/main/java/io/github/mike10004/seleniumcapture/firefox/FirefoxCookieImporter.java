package io.github.mike10004.seleniumcapture.firefox;

import io.github.mike10004.seleniumcapture.DeserializableCookie;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

/**
 * Interface of a service that imports cookies into a Firefox cookies database file.
 */
public interface FirefoxCookieImporter {

    void importCookies(Iterable<DeserializableCookie> cookies, File sqliteDbFile, Path scratchDir) throws SQLException, IOException;

    void createEmptyCookiesDb(File destinationSqliteDbFile) throws IOException;

}
