package com.github.mike10004.seleniumhelp;

import com.github.mike10004.nativehelper.Platform;
import com.github.mike10004.nativehelper.Platforms;
import com.github.mike10004.nativehelper.subprocess.ProcessResult;
import com.github.mike10004.nativehelper.subprocess.Subprocess;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public abstract class Sqlite3Runner {

    private static final Logger log = LoggerFactory.getLogger(Sqlite3Runner.class);
    private static final Charset SQLITE3_CHARSET = Charset.defaultCharset();
    private final ExecutableConfig config;

    protected Sqlite3Runner(ExecutableConfig config) {
        this.config = requireNonNull(config);
    }

    static ProcessResult<String, String> executeOrPropagateInterruption(Subprocess subprocess, @Nullable ByteSource stdinSource) throws ProcessWaitingInterruptedException {
        try {
            return Subprocesses.executeAndWait(subprocess, SQLITE3_CHARSET, stdinSource);
        } catch (InterruptedException e) {
            throw new ProcessWaitingInterruptedException(subprocess, e);
        }
    }

    protected void assertSqlite3Available() throws SQLException {
        if (!config.isExecutableAvailable()) {
            throw new SQLException("no sqlite3 executable found in search of PATH");
        }
    }

    protected Subprocess.Builder getSqlite3Builder() {
        return config.subprocessBuilder();
    }

    static class Sqlite3GenericExporter extends Sqlite3Runner {

        public Sqlite3GenericExporter(ExecutableConfig config) {
            super(config);
        }

        private static final String ALLOWED_TABLE_NAME_REGEX = "[A-Za-z]\\w*";

        public List<Map<String, String>> dumpRows(String tableName, File sqliteDbFile) throws SQLException, IOException {
            checkArgument(tableName.matches(ALLOWED_TABLE_NAME_REGEX), "table name %s must match regex %s", tableName, ALLOWED_TABLE_NAME_REGEX);
            assertSqlite3Available();
            String sql = "SELECT * FROM " + tableName + " WHERE 1";
            Subprocess subprocess = getSqlite3Builder()
                    .arg("-csv")
                    .arg("-header")
                    .arg(sqliteDbFile.getAbsolutePath())
                    .arg(sql)
                    .build();
            ProcessResult<String, String> result = executeOrPropagateInterruption(subprocess, null);
            if (result.exitCode() != 0) {
                log.warn("sqlite3 exited with code {}; stderr: {}", result.exitCode(), result.content().stderr());
                throw new SQLException("sqlite3 exited with code " + result.exitCode() + "; " + StringUtils.abbreviate(result.content().stderr(), 256));
            }
            return Csvs.readRowMaps(CharSource.wrap(result.content().stdout()), Csvs.headersFromFirstRow());
        }

    }

    static class Sqlite3GenericImporter extends Sqlite3Runner {

        protected Sqlite3GenericImporter(ExecutableConfig config) {
            super(config);
        }

        protected static void checkResult(ProcessResult<String, String> result) throws SQLException {
            if (result.exitCode() != 0) {
                log.error("sqlite3 exited with code {}; stderr: {}", result.exitCode(), result.content().stderr());
                throw new SQLException("sqlite3 exited with code " + result.exitCode() + "; " + StringUtils.abbreviate(result.content().stderr(), 256));
            }
        }

        public List<String> queryTableNames(File sqliteDbFile) throws SQLException, IOException {
            Subprocess subprocess = getSqlite3Builder()
                    .arg(sqliteDbFile.getAbsolutePath())
                    .arg(".tables")
                    .build();
            ProcessResult<String, String> result = executeOrPropagateInterruption(subprocess, null);
            checkResult(result);
            List<String> tableNames = CharSource.wrap(result.content().stdout()).readLines();
            return tableNames;
        }

        public Optional<Integer> findMaxValue(File sqliteDbFile, String columnName, String tableName) throws SQLException {
            checkArgument(columnName.matches("[_A-Za-z]\\w*"), "illegal column name: %s", columnName);
            Subprocess subprocess = getSqlite3Builder()
                    .arg("-csv")
                    .arg(sqliteDbFile.getAbsolutePath())
                    .arg("SELECT MAX(" + columnName + ") FROM " + tableName + " WHERE 1")
                    .build();
            ProcessResult<String, String> result = executeOrPropagateInterruption(subprocess, null);
            checkResult(result);
            String output = result.content().stdout().trim();
            if (output.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(Integer.valueOf(output));
            }
        }

        protected static String escapeSqlite3Token(String token) {
            return StringEscapeUtils.escapeJava(token);
        }

        protected Platform getPlatform() {
            return Platforms.getPlatform();
        }

        public void doImportRows(Iterable<Map<String, String>> rows, List<String> sqliteColumnNames, File sqliteDbFile, String tableName, Path scratchDir, String defaultCellValue) throws SQLException, IOException {
            String stdin = Csvs.writeRowMapsToString(sqliteColumnNames, rows, defaultCellValue, Csvs.UnknownKeyStrategy.IGNORE);
            ByteSource stdinSource = CharSource.wrap(stdin).asByteSource(SQLITE3_CHARSET);
            String stdinFilename;
            File inputFile = null;
            try {
                if (getPlatform().isWindows()) {
                    inputFile = File.createTempFile("firefox-cookie-import", ".csv", scratchDir.toFile());
                    stdinSource.copyTo(Files.asByteSink(inputFile));
                    stdinSource = Files.asByteSource(inputFile);
                    stdinFilename = escapeSqlite3Token(inputFile.getAbsolutePath());
                } else {
                    stdinFilename = "/dev/stdin";
                }
                Subprocess program = getSqlite3Builder()
                        .arg("-csv")
                        .arg(sqliteDbFile.getAbsolutePath())
                        .arg(String.format(".import \"%s\" %s", stdinFilename, tableName))
                        .build();
                ProcessResult<String, String> result = executeOrPropagateInterruption(program, stdinSource);
                checkResult(result);
            } finally {
                if (inputFile != null) {
                    if (!inputFile.delete()) {
                        log.warn("failed to delete temporary input file {}", inputFile);
                    }
                }
            }
        }

    }

    private static class ProcessWaitingInterruptedException extends RuntimeException {
        public ProcessWaitingInterruptedException(Subprocess subprocess, InterruptedException e) {
            super("waiting for " + subprocess + " to finish was interrupted", e);
        }
    }
}
