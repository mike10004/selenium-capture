package com.github.mike10004.seleniumhelp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class Firefox68CookieImporter extends FirefoxCookieImporterBase {

    public Firefox68CookieImporter(Sqlite3Runner sqliteRunner, Sqlite3GenericImporter genericImporter) {
        this(sqliteRunner, genericImporter, new StandardCookieExploder(), new Firefox68CookieRowTransform());
    }

    public Firefox68CookieImporter(Sqlite3Runner sqliteRunner, Sqlite3GenericImporter genericImporter, ExplodedCookieConverter explodedCookieConverter, FirefoxCookieRowTransform cookieRowTransform) {
        super(sqliteRunner, genericImporter, getImportInfo(), explodedCookieConverter, cookieRowTransform);
    }

    static final ImmutableList<String> FF68_SQLITE_COLUMN_NAMES = ImmutableList.of(
            "id",
            "baseDomain",
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
            "appId",
            "inBrowserElement");

    @VisibleForTesting
    static final String FF68_TABLE_NAME = "moz_cookies";

    public static Sqlite3ImportInfo getImportInfo() {
        return new Sqlite3ImportInfo() {
            @Override
            public String tableName() {
                return FF68_TABLE_NAME;
            }

            @Override
            public List<String> columnNames() {
                return FF68_SQLITE_COLUMN_NAMES;
            }

            @Override
            public List<String> createTableSqlStatements() {
                return FF68_CREATE_TABLE_SQL;
            }
        };
    }

    @Override
    public String getEmptyDbResourcePath() {
        return "/selenium-capture/firefox/empty-cookies-db-ff68.sqlite";
    }

    private static final ImmutableList<String> FF68_CREATE_TABLE_SQL = ImmutableList.of(
            "CREATE TABLE moz_cookies (" +
                    "id INTEGER PRIMARY KEY, " +
                    "baseDomain TEXT, " +
                    "originAttributes TEXT NOT NULL DEFAULT '', " +
                    "name TEXT, " +
                    "value TEXT, " +
                    "host TEXT, " +
                    "path TEXT, " +
                    "expiry INTEGER, " +
                    "lastAccessed INTEGER, " +
                    "creationTime INTEGER, " +
                    "isSecure INTEGER, " +
                    "isHttpOnly INTEGER, " +
                    "appId INTEGER DEFAULT 0, " +
                    "inBrowserElement INTEGER DEFAULT 0, " +
                    "CONSTRAINT moz_uniqueid UNIQUE (name, host, path, originAttributes)" +
                    ");",
            "CREATE INDEX moz_basedomain ON moz_cookies (baseDomain, originAttributes);"
    );

}

