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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class Sqlite3GenericImporter {

    private static final Logger log = LoggerFactory.getLogger(Sqlite3GenericImporter.class);

    public Sqlite3GenericImporter() {
    }

    protected static String escapeSqlite3Token(String token) {
        return StringEscapeUtils.escapeJava(token);
    }

    protected Platform getPlatform() {
        return Platforms.getPlatform();
    }

    public void doImportRows(Sqlite3Runner sqliteRunner,
                             Iterable<Map<String, String>> rows,
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

}
