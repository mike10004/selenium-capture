package io.github.mike10004.seleniumcapture.firefox;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import io.github.mike10004.seleniumcapture.CookieExploder;
import io.github.mike10004.seleniumcapture.StandardCookieExploder;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

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

    private static final ImmutableList<String> columnNames = ImmutableList.of("id",
            "originAttributes",
            "name",
            "value",
            "host",
            "path",
            "expiry",
            "lastAccessed",
            "creationTime",
            "isSecure",
            "isHttpOnly",
            "inBrowserElement",
            "sameSite",
            "rawSameSite",
            "schemeMap"
            );

    public Firefox91CookieImporter(Sqlite3Runner sqliteRunner, Sqlite3GenericImporter genericImporter) {
        this(sqliteRunner, genericImporter, getImportInfo(), new StandardCookieExploder(), new Firefox91CookieRowTransform());
    }

    public Firefox91CookieImporter(Sqlite3Runner sqliteRunner,
                                   Sqlite3GenericImporter genericImporter,
                                   Sqlite3ImportInfo importInfo,
                                   CookieExploder explodedCookieConverter,
                                   FirefoxCookieRowTransform cookieRowTransform) {
        super(sqliteRunner, genericImporter, importInfo, explodedCookieConverter, cookieRowTransform);
    }

    public static Sqlite3ImportInfo getImportInfo() {
        return new Sqlite3ImportInfo() {
            @Override
            public String tableName() {
                return "moz_cookies";
            }

            @Override
            public List<String> columnNames() {
                return columnNames;
            }

            @Override
            public List<String> createTableSqlStatements() {
                return Collections.singletonList(createTableStmt);
            }
        };
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
