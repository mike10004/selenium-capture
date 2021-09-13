package io.github.mike10004.seleniumcapture.firefox;

import com.google.common.collect.Iterables;
import io.github.mike10004.seleniumcapture.DeserializableCookie;
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

import static java.util.Objects.requireNonNull;

public abstract class FirefoxCookieImporterBase implements FirefoxCookieImporter {

    private static final Logger log = LoggerFactory.getLogger(FirefoxCookieImporterBase.class);

    private final Sqlite3GenericImporter genericImporter;
    private final Sqlite3ImportInfo importInfo;
    private final FirefoxCookieRowTransform cookieRowTransform;

    public FirefoxCookieImporterBase(Sqlite3GenericImporter genericImporter,
                                     Sqlite3ImportInfo importInfo,
                                     FirefoxCookieRowTransform cookieRowTransform) {
        this.genericImporter = requireNonNull(genericImporter);
        this.importInfo = requireNonNull(importInfo);
        this.cookieRowTransform = requireNonNull(cookieRowTransform);
    }

    @Override
    public void importCookies(Iterable<DeserializableCookie> cookies, File sqliteDbFile, Path scratchDir) throws SQLException, IOException {
        log.debug("{} cookies from {} into database", Iterables.size(cookies), sqliteDbFile);
        Iterable<Map<String, String>> rows = StreamSupport.stream(cookies.spliterator(), false)
                .map(cookieRowTransform::apply)
                .collect(Collectors.toList());
        importRows(rows, importInfo, sqliteDbFile, scratchDir);
    }

    private void importRows(Iterable<Map<String, String>> rows, Sqlite3ImportInfo importInfo, File sqliteDbFile, Path scratchDir) throws SQLException, IOException {
        Integer maxIdValue = genericImporter.ensureTableCreated(importInfo, sqliteDbFile);
        if (maxIdValue == null) {
            log.warn("no max id value ascertained from database");
            maxIdValue = 0;
        }
        final AtomicInteger idFactory = new AtomicInteger(maxIdValue);
        List<Map<String, String>> rowsWithIds = new ArrayList<>();
        rows.forEach(row -> rowsWithIds.add(new LinkedHashMap<>(row)));
        for (Map<String, String> row : rowsWithIds) {
            row.put("id", String.valueOf(idFactory.incrementAndGet()));
        }
        genericImporter.doImportRows(rowsWithIds, importInfo, sqliteDbFile, scratchDir);
    }
}
