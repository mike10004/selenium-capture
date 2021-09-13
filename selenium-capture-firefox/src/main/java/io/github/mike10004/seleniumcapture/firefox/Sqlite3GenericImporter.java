package io.github.mike10004.seleniumcapture.firefox;

import com.github.mike10004.nativehelper.Platform;
import com.github.mike10004.nativehelper.Platforms;
import io.github.mike10004.seleniumcapture.Subprocesses;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.Subprocess;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static io.github.mike10004.seleniumcapture.Subprocesses.checkResult;
import static java.util.Objects.requireNonNull;

public class Sqlite3GenericImporter {

    private static final Logger log = LoggerFactory.getLogger(Sqlite3GenericImporter.class);

    private final Sqlite3Runner sqliteRunner;

    public Sqlite3GenericImporter(Sqlite3Runner sqliteRunner) {
        this.sqliteRunner = requireNonNull(sqliteRunner);
    }

    protected static String escapeSqlite3Token(String token) {
        return StringEscapeUtils.escapeJava(token);
    }

    protected Platform getPlatform() {
        return Platforms.getPlatform();
    }

    /**
     *
     * @param sqliteRunner
     * @param importInfo
     * @param sqliteDbFile
     * @return max primary key value, if relevant
     */
    @Nullable
    public Integer ensureTableCreated(Sqlite3ImportInfo importInfo,
                                      File sqliteDbFile) throws SQLException, IOException {
        List<String> tableNames = sqliteRunner.queryTableNames(sqliteDbFile);
        final Integer maxIdValue;
        if (!tableNames.contains(importInfo.tableName())) {
            createTable(importInfo, sqliteDbFile);
            maxIdValue = 0;
        } else {
            String idColumnName = importInfo.idColumnName();
            if (idColumnName == null) {
                maxIdValue = null;
            } else {
                maxIdValue = sqliteRunner.findMaxValue(sqliteDbFile, idColumnName, importInfo.tableName()).orElse(0);
            }
        }
        return maxIdValue;
    }

    public void doImportRows(Iterable<Map<String, String>> rows,
                             Sqlite3ImportInfo importInfo,
                             File sqliteDbFile,
                             Path scratchDir) throws SQLException, IOException {
        String stdin = Csvs.writeRowMapsWithHeadersToString(importInfo.columnNames(), rows, importInfo.defaultCellValue(), Csvs.UnknownKeyStrategy.IGNORE);
        CharSource stdinSource = CharSource.wrap(stdin);
        String stdinFilename;
        File inputFile = null;
        try {
            if (getPlatform().isWindows()) {
                inputFile = File.createTempFile("firefox-cookie-import", ".csv", scratchDir.toFile());
                stdinSource.copyTo(Files.asByteSink(inputFile).asCharSink(sqliteRunner.config.getEncoding()));
                stdinSource = null;
                stdinFilename = escapeSqlite3Token(inputFile.getAbsolutePath());
            } else {
                stdinFilename = "/dev/stdin";
            }
            Subprocess program = sqliteRunner.getSqlite3Builder()
                    .arg("-csv")
                    .arg(sqliteDbFile.getAbsolutePath())
                    .arg(String.format(".import \"%s\" %s", stdinFilename, importInfo.tableName()))
                    .build();
            ProcessResult<String, String> result = sqliteRunner.executeOrPropagateInterruption(program, stdinSource);
            Subprocesses.checkResult(result);
        } finally {
            if (inputFile != null) {
                if (!inputFile.delete()) {
                    log.warn("failed to delete temporary input file {}", inputFile);
                }
            }
        }
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

}
