package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("Guava")
public class FirefoxCookieDbTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void getImporter() {
        assertNotNull(FirefoxCookieDb.getImporter());
    }

    @Test
    public void getExporter() {
        assertNotNull(FirefoxCookieDb.getExporter());
    }

    @Test
    public void Sqlite3ProgramImporter_importRows() throws Exception {
        Map<String, String> cookieFieldMap = Iterables.getOnlyElement(Csvs.readRowMaps(CharSource.wrap(ExampleCookieSource.csvText), Csvs.headersFromFirstRow()));
        Map<String, String> export1 = importAndCheck(cookieFieldMap);
        Map<String, String> export2 = importAndCheck(export1);
        System.out.format("re-exported: %s%n", export2);
    }

    private Map<String, String> importAndCheck(Map<String, String> cookieFieldMap) throws IOException, SQLException {
        FirefoxCookieDb.Sqlite3ProgramImporter importer = new FirefoxCookieDb.Sqlite3ProgramImporter();
        File dbFile = tmp.newFile();
        importer.importRows(ImmutableList.of(cookieFieldMap), dbFile);
        Map<String, String> exportedCookieFieldMap = Iterables.getOnlyElement(new FirefoxCookieDb.Sqlite3ProgramExporter().dumpRows(dbFile));
        assertThat("field map", exportedCookieFieldMap, new MapMatcher<String, String>(cookieFieldMap) {
            @Override
            protected boolean isIgnoreValueEquality(Object key, Object expectedValue, Object actualValue) {
                return "id".equals(key);
            }
        });
        return exportedCookieFieldMap;
    }



    @Test
    public void Sqlite3ProgramExporter_dumpRows() throws Exception {
        File dbFile = tmp.newFile();
        Resources.asByteSource(getClass().getResource("/firefox-cookies-db-with-google-cookie.sqlite"))
                .copyTo(Files.asByteSink(dbFile));
        FirefoxCookieDb.Sqlite3ProgramExporter exporter = new FirefoxCookieDb.Sqlite3ProgramExporter();
        List<Map<String, String>> rowMaps = exporter.dumpRows(dbFile);
        assertEquals("rowMaps.size", 1, rowMaps.size());
        Map<String, String> rowMap = rowMaps.iterator().next();
        Map<String, String> groundTruth = Iterables.getOnlyElement(Csvs.readRowMaps(CharSource.wrap(ExampleCookieSource.csvText), Csvs.headersFromFirstRow()));
        assertEquals("exported map", groundTruth, rowMap);
    }

}