package com.github.mike10004.seleniumhelp;

import com.github.mike10004.nativehelper.subprocess.ProcessResult;
import com.github.mike10004.nativehelper.subprocess.Subprocess;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"Convert2Lambda"})
public class FirefoxCookieDb {

    private FirefoxCookieDb() {}

    static final ImmutableList<String> SQLITE_COLUMN_NAMES = ImmutableList.of(
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
    static final String TABLE_NAME = "moz_cookies";

    public static class CookieTransferConfig extends ExecutableConfig.BasicExecutableConfig {

        private static final String SQLITE3_EXECUTABLE_NAME = "sqlite3";

        public CookieTransferConfig(@Nullable File executablePathname, String executableFilename) {
            super(executablePathname, executableFilename);
        }

        @SuppressWarnings("unused")
        public static CookieTransferConfig forSqlite3Executable(File executable) {
            return new CookieTransferConfig(executable, SQLITE3_EXECUTABLE_NAME);
        }

        public static CookieTransferConfig createDefault() {
            return new CookieTransferConfig(null, SQLITE3_EXECUTABLE_NAME);
        }

    }

    public static Importer getImporter() {
        return getImporter(CookieTransferConfig.createDefault());
    }

    public static Exporter getExporter() {
        return getExporter(CookieTransferConfig.createDefault());
    }

    public static Importer getImporter(CookieTransferConfig config) {
        return new Sqlite3ProgramImporter(config);
    }

    public static Exporter getExporter(CookieTransferConfig config) {
        return new Sqlite3ProgramExporter(config);
    }

    public interface Exporter {
        List<DeserializableCookie> exportCookies(File sqliteDbFile) throws SQLException, IOException;
    }

    public interface Importer {
        void importCookies(Iterable<DeserializableCookie> cookies, File sqliteDbFile, Path scratchDir) throws SQLException, IOException;
    }

    @VisibleForTesting
    static class Sqlite3ProgramExporter extends Sqlite3Runner.Sqlite3GenericExporter implements Exporter {

        private static final Logger log = LoggerFactory.getLogger(Sqlite3ProgramExporter.class);

        public Sqlite3ProgramExporter(CookieTransferConfig config) {
            super(config);
        }

        public List<DeserializableCookie> exportCookies(File sqliteDbFile) throws SQLException, IOException {
            log.debug("cookies going to {}", sqliteDbFile);
            List<Map<String, String>> cookiesDbRows = dumpRows(TABLE_NAME, sqliteDbFile);
            return cookiesDbRows.stream().map(this::makeCookie)
                    .collect(ImmutableList.toImmutableList());
        }

        /**
         * Creates a cookie from the data in a map that represents a row of the
         * Firefox cookies database.
         * @param row the row of the database, mapping field names to field values
         * @return a cookie
         */
        protected DeserializableCookie makeCookie(Map<String, String> row) {
            // TODO construct DeserializableCookie from map
            throw new UnsupportedOperationException("not yet supported: convert instance of "
                    + row.getClass() + " to instance of " + DeserializableCookie.class);
        }

    }

    private static final String DEFAULT_SQLITE_CELL_VALUE = "";

    @VisibleForTesting
    static class Sqlite3ProgramImporter extends Sqlite3Runner.Sqlite3GenericImporter implements Importer {

        private static final Logger log = LoggerFactory.getLogger(Sqlite3ProgramImporter.class);

        private static final ImmutableList<String> CREATE_TABLE_SQL = ImmutableList.of(
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

        public Sqlite3ProgramImporter(CookieTransferConfig config) {
            super(config);
        }

        @Override
        public void importCookies(Iterable<DeserializableCookie> cookies, File sqliteDbFile, Path scratchDir) throws SQLException, IOException {
            log.debug("{} cookies from {} into database", Iterables.size(cookies), sqliteDbFile);
            Converter<DeserializableCookie, Map<String, Object>> cookieExploder = new ExplodedCookieConverter().reverse();
            Converter<Map<String, Object>, Map<String, String>> sqlRowMapper = new FirefoxCookieRowTransform().asConverter();
            Converter<DeserializableCookie, Map<String, String>> cookieToRowTransform = cookieExploder.andThen(sqlRowMapper);
            Iterable<Map<String, String>> rows = Iterables.transform(cookies, cookieToRowTransform);
            importRows(rows, sqliteDbFile, scratchDir);
        }

        private void createTable(File sqliteDbFile) throws SQLException {
            // sqlite3 3.11 allows multiple sql statement arguments, but
            // version 3.8 requires executing once per statement argument
            for (String stmt : CREATE_TABLE_SQL) {
                Subprocess createTableProgram = getSqlite3Builder()
                        .arg(sqliteDbFile.getAbsolutePath())
                        .arg(stmt)
                        .build();
                ProcessResult<String, String> createTableResult = executeOrPropagateInterruption(createTableProgram, null);
                checkResult(createTableResult);
            }
        }

        public void importRows(Iterable<Map<String, String>> rows, File sqliteDbFile, Path scratchDir) throws SQLException, IOException {
            List<String> tableNames = queryTableNames(sqliteDbFile);
            final int maxIdValue;
            if (!tableNames.contains(TABLE_NAME)) {
                createTable(sqliteDbFile);
                maxIdValue = 0;
            } else {
                maxIdValue = findMaxValue(sqliteDbFile, "id", TABLE_NAME).orElse(0);
            }
            final AtomicInteger idFactory = new AtomicInteger(maxIdValue);
            List<Map<String, String>> rowsWithIds = new ArrayList<>();
            rows.forEach(row -> rowsWithIds.add(new LinkedHashMap<>(row)));
            for (Map<String, String> row : rowsWithIds) {
                row.put("id", String.valueOf(idFactory.incrementAndGet()));
            }
            doImportRows(rowsWithIds, SQLITE_COLUMN_NAMES, sqliteDbFile, TABLE_NAME, scratchDir, DEFAULT_SQLITE_CELL_VALUE);
        }

    }

}
