package com.github.mike10004.seleniumhelp;

import com.github.mike10004.nativehelper.Program;
import com.github.mike10004.nativehelper.ProgramWithOutputStrings;
import com.github.mike10004.nativehelper.ProgramWithOutputStringsResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Converter;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import com.novetta.ibg.common.sys.Whicher;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;

@SuppressWarnings({"Guava", "Convert2Lambda"})
public abstract class FirefoxCookieDb {

    private FirefoxCookieDb() {}

    public static Importer getImporter() {
        return new Sqlite3ProgramImporter();
    }

    public static Exporter getExporter() {
        return new Sqlite3ProgramExporter();
    }

    static final ImmutableList<String> sqliteColumnNames = ImmutableList.of(
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

    private static final String TABLE_NAME = "moz_cookies";

    private static final Logger log = LoggerFactory.getLogger(FirefoxCookieDb.class);

    protected static boolean isSqlite3PresentOnSystemPath() {
        return Whicher.gnu().which("sqlite3").isPresent();
    }

    public interface Exporter {
        List<DeserializableCookie> exportCookies(File sqliteDbFile) throws SQLException, IOException;
    }

    public interface Importer {
        void importCookies(Iterable<DeserializableCookie> cookies, File sqliteDbFile) throws SQLException, IOException;
    }

    @VisibleForTesting
    static class Sqlite3ProgramExporter implements Exporter {

        @Override
        public List<DeserializableCookie> exportCookies(File sqliteDbFile) throws SQLException, IOException {
            throw new UnsupportedOperationException("not yet supported");
        }

        public List<Map<String, String>> dumpRows(File sqliteDbFile) throws SQLException, IOException {
            String sql = "SELECT * FROM " + TABLE_NAME + " WHERE 1";
            if (!isSqlite3PresentOnSystemPath()) {
                throw new SQLException("no sqlite3 executable found in search of PATH");
            }
            ProgramWithOutputStringsResult result = Program.running("sqlite3").arg("-csv").arg("-header").arg(sqliteDbFile.getAbsolutePath()).arg(sql).outputToStrings().execute();
            if (result.getExitCode() != 0) {
                log.warn("sqlite3 exited with code {}; stderr: {}", result.getExitCode(), result.getStderrString());
                throw new SQLException("sqlite3 exited with code " + result.getExitCode() + "; " + StringUtils.abbreviate(result.getStderrString(), 256));
            }
            return Csvs.readRowMaps(CharSource.wrap(result.getStdoutString()), Csvs.headersFromFirstRow());
        }

    }

    private static final String DEFAULT_SQLITE_CELL_VALUE = "";

    @VisibleForTesting
    static class Sqlite3ProgramImporter implements Importer {

        @Override
        public void importCookies(Iterable<DeserializableCookie> cookies, File sqliteDbFile) throws SQLException, IOException {
            Converter<DeserializableCookie, Map<String, Object>> cookieExploder = new ExplodedCookieConverter().reverse();
            Converter<Map<String, Object>, Map<String, String>> sqlRowMapper = new FirefoxCookieRowTransform().asConverter();
            Converter<DeserializableCookie, Map<String, String>> cookieToRowTransform = cookieExploder.andThen(sqlRowMapper);
            Iterable<Map<String, String>> rows = Iterables.transform(cookies, cookieToRowTransform);
            importRows(rows, sqliteDbFile);
        }

        private static final ImmutableList<String> CREATE_TABLE_SQL = ImmutableList.of(
                "CREATE TABLE moz_cookies (id INTEGER PRIMARY KEY, baseDomain TEXT, originAttributes TEXT NOT NULL DEFAULT '', name TEXT, value TEXT, host TEXT, path TEXT, expiry INTEGER, lastAccessed INTEGER, creationTime INTEGER, isSecure INTEGER, isHttpOnly INTEGER, appId INTEGER DEFAULT 0, inBrowserElement INTEGER DEFAULT 0, CONSTRAINT moz_uniqueid UNIQUE (name, host, path, originAttributes));",
                "CREATE INDEX moz_basedomain ON moz_cookies (baseDomain, originAttributes);"
        );

        private static void checkResult(ProgramWithOutputStringsResult result) throws SQLException, IOException {
            if (result.getExitCode() != 0) {
                log.error("sqlite3 exited with code {}; stderr: {}", result.getExitCode(), result.getStderrString());
                throw new SQLException("sqlite3 exited with code " + result.getExitCode() + "; " + StringUtils.abbreviate(result.getStderrString(), 256));
            }
        }

        private void createTable(File sqliteDbFile) throws SQLException, IOException {
            // sqlite3 3.11 allows multiple sql statement arguments, but
            // version 3.8 requires executing once per statement argument
            for (String stmt : CREATE_TABLE_SQL) {
                ProgramWithOutputStrings createTableProgram = Program.running(resolveSqlite3Executable())
                        .arg(sqliteDbFile.getAbsolutePath())
                        .arg(stmt)
                        .outputToStrings();
                ProgramWithOutputStringsResult createTableResult = createTableProgram.execute();
                checkResult(createTableResult);
            }
        }

        private List<String> queryTableNames(File sqliteDbFile) throws SQLException, IOException {
            ProgramWithOutputStringsResult result = Program.running(resolveSqlite3Executable())
                    .arg(sqliteDbFile.getAbsolutePath())
                    .arg(".tables")
                    .outputToStrings()
                    .execute();
            checkResult(result);
            List<String> tableNames = CharSource.wrap(result.getStdoutString()).readLines();
            return tableNames;
        }

        @SuppressWarnings("SameParameterValue")
        private Optional<Integer> findMaxValue(File sqliteDbFile, String columnName) throws SQLException, IOException {
            checkArgument(columnName.matches("[_A-Za-z]\\w*"), "illegal column name: %s", columnName);
            ProgramWithOutputStringsResult result = Program.running(resolveSqlite3Executable())
                    .arg("-csv")
                    .arg(sqliteDbFile.getAbsolutePath())
                    .arg("SELECT MAX(" + columnName + ") FROM " + TABLE_NAME + " WHERE 1")
                    .outputToStrings()
                    .execute();
            checkResult(result);
            String output = result.getStdoutString().trim();
            if (output.isEmpty()) {
                return Optional.absent();
            } else {
                return Optional.of(Integer.valueOf(output));
            }
        }

        private void doImportRows(Iterable<Map<String, String>> rows, File sqliteDbFile) throws SQLException, IOException {
            String stdin = Csvs.writeRowMapsToString(sqliteColumnNames, rows, DEFAULT_SQLITE_CELL_VALUE, Csvs.UnknownKeyStrategy.IGNORE);
            ProgramWithOutputStrings program = Program.running(resolveSqlite3Executable())
                    .reading(stdin)
                    .arg("-csv")
                    .arg(sqliteDbFile.getAbsolutePath())
                    .args(".import /dev/stdin " + TABLE_NAME)
                    .outputToStrings();
            ProgramWithOutputStringsResult result = program.execute();
            checkResult(result);
        }

        public void importRows(Iterable<Map<String, String>> rows, File sqliteDbFile) throws SQLException, IOException {
            List<String> tableNames = queryTableNames(sqliteDbFile);
            final int maxIdValue;
            if (!tableNames.contains(TABLE_NAME)) {
                createTable(sqliteDbFile);
                maxIdValue = 0;
            } else {
                maxIdValue = findMaxValue(sqliteDbFile, "id").or(0);
            }
            final AtomicInteger idFactory = new AtomicInteger(maxIdValue);
            List<Map<String, String>> rowsWithIds = new ArrayList<>();
            rows.forEach(row -> rowsWithIds.add(new LinkedHashMap<>(row)));
            for (Map<String, String> row : rowsWithIds) {
                row.put("id", String.valueOf(idFactory.incrementAndGet()));
            }
            doImportRows(rowsWithIds, sqliteDbFile);
        }

        protected File resolveSqlite3Executable() throws IOException {
            Optional<File> file = Whicher.gnu().which("sqlite3");
            if (file.isPresent()) {
                return file.get();
            }
            throw new FileNotFoundException("sqlite3 executable");
        }

    }

}
