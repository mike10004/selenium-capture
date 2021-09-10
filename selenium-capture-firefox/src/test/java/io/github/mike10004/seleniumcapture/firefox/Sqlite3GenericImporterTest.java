package io.github.mike10004.seleniumcapture.firefox;

import com.github.mike10004.seleniumhelp.ExecutableConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.CharSource;
import io.github.mike10004.seleniumcapture.testing.MapMatcher;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class Sqlite3GenericImporterTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void importRows() throws Exception {
        Map<String, String> cookieFieldMap = Iterables.getOnlyElement(Csvs.readRowMaps(CharSource.wrap(ExampleCookieSource.getCsvText_FF91()), Csvs.headersFromFirstRow()));
        Map<String, String> export1 = importAndCheck(Firefox91CookieImporter.getImportInfo(), cookieFieldMap);
        Map<String, String> export2 = importAndCheck(Firefox91CookieImporter.getImportInfo(), export1);
        System.out.format("re-exported: %s%n", export2);
    }

    private Map<String, String> importAndCheck(Sqlite3ImportInfo importInfo, Map<String, String> cookieFieldMap) throws IOException, SQLException {
        ExecutableConfig config = Sqlite3Runner.createDefaultSqlite3Config();
        Sqlite3GenericImporter importer = new Sqlite3GenericImporter();
        File dbFile = tmp.newFile();
        importer.doImportRows(new Sqlite3Runner(config), ImmutableList.of(cookieFieldMap), importInfo, dbFile, tmp.getRoot().toPath());
        Map<String, String> exportedCookieFieldMap = Iterables.getOnlyElement(
                new Sqlite3GenericExporter(new Sqlite3Runner(config)).dumpRows("moz_cookies", dbFile));
        MatcherAssert.assertThat("field map", exportedCookieFieldMap, new MapMatcher<String, String>(cookieFieldMap) {
            @Override
            protected boolean isIgnoreValueEquality(Object key, Object expectedValue, Object actualValue) {
                return "id".equals(key);
            }
        });
        return exportedCookieFieldMap;
    }
}
