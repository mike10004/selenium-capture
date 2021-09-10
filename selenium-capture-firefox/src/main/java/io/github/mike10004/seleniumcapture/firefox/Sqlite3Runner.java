package io.github.mike10004.seleniumcapture.firefox;

import com.github.mike10004.seleniumhelp.ExecutableConfig;
import com.github.mike10004.seleniumhelp.ProcessWaitingInterruptedException;
import com.github.mike10004.seleniumhelp.StringExecutableConfig;
import com.github.mike10004.seleniumhelp.Subprocesses;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.Subprocess;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class Sqlite3Runner {

    private static final String SQLITE3_EXECUTABLE_NAME = "sqlite3";

    public static ExecutableConfig createDefaultSqlite3Config() {
        return new StringExecutableConfig(SQLITE3_EXECUTABLE_NAME);
    }

    protected final ExecutableConfig config;

    protected Sqlite3Runner(ExecutableConfig config) {
        this.config = requireNonNull(config);
    }

    public static Sqlite3Runner createDefault() {
        return new Sqlite3Runner(createDefaultSqlite3Config());
    }

    protected void assertSqlite3Available() throws SQLException {
        if (!config.isExecutableAvailable()) {
            throw new SQLException("no sqlite3 executable found in search of PATH");
        }
    }

    public ExecutableConfig getConfig() {
        return config;
    }

    public ProcessResult<String, String> executeOrPropagateInterruption(Subprocess subprocess) throws ProcessWaitingInterruptedException {
        return executeOrPropagateInterruption(subprocess, (ByteSource) null);
    }

    public ProcessResult<String, String> executeOrPropagateInterruption(Subprocess subprocess,
                                                                       @Nullable ByteSource stdinSource) throws ProcessWaitingInterruptedException {
        return Subprocesses.executeOrPropagateInterruption(subprocess, config.getEncoding(), stdinSource);
    }

    public ProcessResult<String, String> executeOrPropagateInterruption(Subprocess subprocess,
                                                                       @Nullable CharSource stdinSource) throws ProcessWaitingInterruptedException {
        return Subprocesses.executeOrPropagateInterruption(subprocess, config.getEncoding(), stdinSource == null ? null : stdinSource.asByteSource(config.getEncoding()));
    }

    Subprocess.Builder getSqlite3Builder() {
        return Subprocess.running(config.getExecutableName());
    }

    public List<String> queryTableNames(File sqliteDbFile) throws SQLException, IOException {
        Subprocess subprocess = getSqlite3Builder()
                .arg(sqliteDbFile.getAbsolutePath())
                .arg(".tables")
                .build();
        ProcessResult<String, String> result = Subprocesses.executeOrPropagateInterruption(subprocess, config.getEncoding(), null);
        Subprocesses.checkResult(result);
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
        ProcessResult<String, String> result = executeOrPropagateInterruption(subprocess);
        Subprocesses.checkResult(result);
        String output = result.content().stdout().trim();
        if (output.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(Integer.valueOf(output));
        }
    }

}
