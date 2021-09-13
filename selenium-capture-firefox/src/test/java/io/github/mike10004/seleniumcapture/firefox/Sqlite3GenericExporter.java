package io.github.mike10004.seleniumcapture.firefox;

import com.google.common.io.CharSource;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.Subprocess;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

class Sqlite3GenericExporter {

    private static final Logger log = LoggerFactory.getLogger(Sqlite3GenericExporter.class);

    private final Sqlite3Runner sqliteRunner;

    public Sqlite3GenericExporter(Sqlite3Runner sqliteRunner) {
        this.sqliteRunner = requireNonNull(sqliteRunner);
    }

    private static final String ALLOWED_TABLE_NAME_REGEX = "[A-Za-z]\\w*";

    public List<Map<String, String>> dumpRows(String tableName, File sqliteDbFile) throws SQLException, IOException {
        checkArgument(tableName.matches(ALLOWED_TABLE_NAME_REGEX), "table name %s must match regex %s", tableName, ALLOWED_TABLE_NAME_REGEX);
        sqliteRunner.assertSqlite3Available();
        String sql = "SELECT * FROM " + tableName + " WHERE 1";
        Subprocess subprocess = sqliteRunner.getSqlite3Builder()
                .arg("-csv")
                .arg("-header")
                .arg(sqliteDbFile.getAbsolutePath())
                .arg(sql)
                .build();
        ProcessResult<String, String> result = sqliteRunner.executeOrPropagateInterruption(subprocess);
        if (result.exitCode() != 0) {
            log.warn("sqlite3 exited with code {}; stderr: {}", result.exitCode(), result.content().stderr());
            throw new SQLException("sqlite3 exited with code " + result.exitCode() + "; " + StringUtils.abbreviate(result.content().stderr(), 256));
        }
        return Csvs.readRowMaps(CharSource.wrap(result.content().stdout()), Csvs.headersFromFirstRow());
    }

}
