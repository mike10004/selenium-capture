package io.github.mike10004.seleniumcapture.firefox;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import static java.util.Objects.requireNonNull;

public class Firefox91CookieImporter extends FirefoxCookieImporterBase {

    private static final String createTableStmt = "" +
            "CREATE TABLE IF NOT EXISTS \"moz_cookies\" (\n" +
            "  \"id\"  INTEGER,\n" +
            "  \"originAttributes\"  TEXT NOT NULL DEFAULT '',\n" +
            "  \"name\"  TEXT,\n" +
            "  \"value\"  TEXT,\n" +
            "  \"host\"  TEXT,\n" +
            "  \"path\"  TEXT,\n" +
            "  \"expiry\"  INTEGER,\n" +
            "  \"lastAccessed\"  INTEGER,\n" +
            "  \"creationTime\"  INTEGER,\n" +
            "  \"isSecure\"  INTEGER,\n" +
            "  \"isHttpOnly\"  INTEGER,\n" +
            "  \"inBrowserElement\"  INTEGER DEFAULT 0,\n" +
            "  \"sameSite\"  INTEGER DEFAULT 0,\n" +
            "  \"rawSameSite\"  INTEGER DEFAULT 0,\n" +
            "  \"schemeMap\"  INTEGER DEFAULT 0,\n" +
            "  CONSTRAINT \"moz_uniqueid\" UNIQUE(\"name\",\"host\",\"path\",\"originAttributes\"),\n" +
            "  PRIMARY KEY(\"id\")\n" +
            ")";

    public static final String COL_NAME = "name";
    public static final String COL_SAMESITE = "sameSite";
    public static final String COL_EXPIRY = "expiry";
    public static final String COL_HOST = "host";
    public static final String COL_IS_HTTP_ONLY = "isHttpOnly";

    private static final ImmutableList<String> columnNames = ImmutableList.of("id",
            "originAttributes",
            COL_NAME,
            "value",
            COL_HOST,
            "path",
            COL_EXPIRY,
            "lastAccessed",
            "creationTime",
            "isSecure",
            COL_IS_HTTP_ONLY,
            "inBrowserElement",
            COL_SAMESITE,
            "rawSameSite",
            "schemeMap"
            );

    public Firefox91CookieImporter(Sqlite3GenericImporter genericImporter) {
        this(genericImporter, getImportInfo(), new Firefox91CookieRowTransform());
    }

    public Firefox91CookieImporter(Sqlite3GenericImporter genericImporter,
                                   Sqlite3ImportInfo importInfo,
                                   FirefoxCookieRowTransform cookieRowTransform) {
        super(genericImporter, importInfo, cookieRowTransform);
    }

    public static Sqlite3ImportInfo getImportInfo() {
        return Sqlite3ImportInfo.create("moz_cookies",
                columnNames, Collections.singletonList(createTableStmt), "id");
    }

    @Override
    public void createEmptyCookiesDb(File destinationSqliteDbFile) throws IOException {
        String resourcePath = getEmptyDbResourcePath();
        URL resource = getClass().getResource(resourcePath);
        requireNonNull(resource, "not found: classpath:" + resourcePath);
        Resources.asByteSource(resource).copyTo(Files.asByteSink(destinationSqliteDbFile));
    }

    private String getEmptyDbResourcePath() {
        return "/selenium-capture/firefox/empty-cookies-db-ff91.sqlite";
    }
}
