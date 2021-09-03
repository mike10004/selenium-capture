package com.github.mike10004.seleniumhelp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.Subprocess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.github.mike10004.seleniumhelp.Subprocesses.checkResult;
import static java.util.Objects.requireNonNull;

public class Firefox68CookieImporter implements FirefoxCookieImporter {

    private static final Logger log = LoggerFactory.getLogger(Firefox68CookieImporter.class);

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

    public Firefox68CookieImporter(Sqlite3Runner sqliteRunner, Sqlite3GenericImporter genericImporter) {
        this(sqliteRunner, genericImporter, new Firefox68ExplodedCookieConverter(), new Firefox68CookieRowTransform());
    }

    private final Sqlite3Runner sqliteRunner;
    private final Sqlite3GenericImporter genericImporter;
    private final ExplodedCookieConverter explodedCookieConverter;
    private final FirefoxCookieRowTransform cookieRowTransform;

    public Firefox68CookieImporter(Sqlite3Runner sqliteRunner, Sqlite3GenericImporter genericImporter, ExplodedCookieConverter explodedCookieConverter, FirefoxCookieRowTransform cookieRowTransform) {
        this.genericImporter = requireNonNull(genericImporter);
        this.sqliteRunner = requireNonNull(sqliteRunner);
        this.explodedCookieConverter = requireNonNull(explodedCookieConverter);
        this.cookieRowTransform = requireNonNull(cookieRowTransform);
    }

    @Override
    public String getEmptyDbResourcePath() {
        return "/empty-firefox-cookies-db.sqlite";
    }

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

    @Override
    public void importCookies(Iterable<DeserializableCookie> cookies, File sqliteDbFile, Path scratchDir) throws SQLException, IOException {
        Sqlite3ImportInfo importInfo = getImportInfo();
        importCookies(importInfo, cookies, sqliteDbFile, scratchDir);
    }

    private void importCookies(Sqlite3ImportInfo importInfo, Iterable<DeserializableCookie> cookies, File sqliteDbFile, Path scratchDir) throws SQLException, IOException {
        log.debug("{} cookies from {} into database", Iterables.size(cookies), sqliteDbFile);
        List<String> columnNames = importInfo.columnNames();
        Iterable<Map<String, String>> rows = StreamSupport.stream(cookies.spliterator(), false)
                        .map(cookie -> {
                            Map<String, Object> explosion = explodedCookieConverter.explode(cookie);
                            return cookieRowTransform.apply(columnNames, explosion);
                        }).collect(Collectors.toList());
        importRows(rows, importInfo, sqliteDbFile, scratchDir);
    }

    private void createTable(Sqlite3ImportInfo importInfo, File sqliteDbFile) throws SQLException {
        // sqlite3 3.11 allows multiple sql statement arguments, but
        // version 3.8 requires executing once per statement argument
        for (String stmt : importInfo.createTableSqlStatements()) {
            Subprocess createTableProgram = sqliteRunner.getSqlite3Builder()
                    .arg(sqliteDbFile.getAbsolutePath())
                    .arg(stmt)
                    .build();
            ProcessResult<String, String> createTableResult =
                    sqliteRunner.executeOrPropagateInterruption(createTableProgram);
            checkResult(createTableResult);
        }
    }

    private void importRows(Iterable<Map<String, String>> rows, Sqlite3ImportInfo importInfo, File sqliteDbFile, Path scratchDir) throws SQLException, IOException {
        List<String> tableNames = sqliteRunner.queryTableNames(sqliteDbFile);
        final int maxIdValue;
        if (!tableNames.contains(importInfo.tableName())) {
            createTable(importInfo, sqliteDbFile);
            maxIdValue = 0;
        } else {
            maxIdValue = sqliteRunner.findMaxValue(sqliteDbFile, "id", importInfo.tableName()).orElse(0);
        }
        final AtomicInteger idFactory = new AtomicInteger(maxIdValue);
        List<Map<String, String>> rowsWithIds = new ArrayList<>();
        rows.forEach(row -> rowsWithIds.add(new LinkedHashMap<>(row)));
        for (Map<String, String> row : rowsWithIds) {
            row.put("id", String.valueOf(idFactory.incrementAndGet()));
        }
        genericImporter.doImportRows(sqliteRunner, rowsWithIds, importInfo, sqliteDbFile, scratchDir);
    }
}
