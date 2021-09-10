package io.github.mike10004.seleniumcapture.firefox;

import io.github.mike10004.seleniumcapture.DeserializableCookie;
import io.github.mike10004.seleniumcapture.ExplodedCookieConverter;
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

import static io.github.mike10004.seleniumcapture.Subprocesses.checkResult;
import static java.util.Objects.requireNonNull;

public abstract class FirefoxCookieImporterBase implements FirefoxCookieImporter {

    private static final Logger log = LoggerFactory.getLogger(FirefoxCookieImporterBase.class);

    private final Sqlite3Runner sqliteRunner;
    private final Sqlite3GenericImporter genericImporter;
    private final Sqlite3ImportInfo importInfo;
    private final ExplodedCookieConverter explodedCookieConverter;
    private final FirefoxCookieRowTransform cookieRowTransform;

    public FirefoxCookieImporterBase(Sqlite3Runner sqliteRunner, Sqlite3GenericImporter genericImporter, Sqlite3ImportInfo importInfo, ExplodedCookieConverter explodedCookieConverter, FirefoxCookieRowTransform cookieRowTransform) {
        this.genericImporter = requireNonNull(genericImporter);
        this.sqliteRunner = requireNonNull(sqliteRunner);
        this.importInfo = requireNonNull(importInfo);
        this.explodedCookieConverter = requireNonNull(explodedCookieConverter);
        this.cookieRowTransform = requireNonNull(cookieRowTransform);
    }

    @Override
    public void importCookies(Iterable<DeserializableCookie> cookies, File sqliteDbFile, Path scratchDir) throws SQLException, IOException {
        log.debug("{} cookies from {} into database", Iterables.size(cookies), sqliteDbFile);
        Iterable<Map<String, String>> rows = StreamSupport.stream(cookies.spliterator(), false)
                .map(cookie -> {
                    Map<String, Object> explosion = explodedCookieConverter.explode(cookie);
                    return cookieRowTransform.apply(explosion);
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
