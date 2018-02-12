package com.github.mike10004.seleniumhelp;

import com.github.mike10004.nativehelper.Platforms;
import com.github.mike10004.nativehelper.subprocess.ProcessResult;
import com.github.mike10004.nativehelper.subprocess.Subprocess;
import com.google.common.collect.ImmutableMap;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Sqlite3RunnerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void executeOrPropagateInterruption() throws Exception {
        Sqlite3Runner runner = createRunner();
        File sqliteDbFile = tmp.newFile();
        createSampleDb(runner, sqliteDbFile);
        assertTrue("nonempty file", sqliteDbFile.length() > 1);
    }

    private Sqlite3Runner createRunner() {
        return new Sqlite3Runner(createConfig()) {};
    }

    private ExecutableConfig createConfig() {
        return FirefoxCookieDb.CookieTransferConfig.createDefault();
    }

    /**
     * Tests that we correctly escape a token inside an sqlite meta-command.
     * I'm not sure what the escaping requirements are exactly, so we take our best guess
     * and test it by actually doing an import from a csv file.
     */
    @Test
    public void escapeSqlite3Token() throws Exception {
        Assume.assumeTrue("only makes sense on windows", Platforms.getPlatform().isWindows());
        File folder = tmp.newFolder();
        String[] filenames = {
                "regular.sqlite",
                "filename with spaces.sqlite",
                "filename \"with\" double quotes.sqlite",
                "filename \'with\' single quotes.sqlite",
                "filename `with` crooked quotes.sqlite",
        };
        Map<Integer, String> idToValue = ImmutableMap.<Integer, String>builder()
                .put(1, "apple")
                .put(2, "orange")
                .put(3, "pear")
                .put(8, "kiwi")
                .build();
        for (String filename : filenames) {
            File sqliteDbFile = new File(folder, filename);
            createSampleDb(createRunner(), sqliteDbFile);
            Sqlite3Runner.Sqlite3GenericImporter importer = new Sqlite3Runner.Sqlite3GenericImporter(createConfig());
            populateSampleDb(importer, idToValue, sqliteDbFile);
            Sqlite3Runner.Sqlite3GenericExporter exporter = new Sqlite3Runner.Sqlite3GenericExporter(createConfig());
            List<Map<String, String>> rows = exporter.dumpRows(SAMPLE_DB_TABLE_NAME, sqliteDbFile);
            // System.out.format("rows: %s%n", rows);
            Map<Integer, String> exported = rows.stream().collect(Collectors.toMap(row -> Integer.valueOf(row.get("id")), row -> row.get("value")));
            assertEquals("exported", idToValue, exported);
        }
    }

    private static final String SAMPLE_DB_TABLE_NAME = "foo";

    @SuppressWarnings("UnusedReturnValue")
    private File createSampleDb(Sqlite3Runner runner, File sqliteDbFile) throws SQLException {
        String createTableSql = "CREATE TABLE " + SAMPLE_DB_TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY, " +
                "value TEXT" +
                ");";
        Subprocess createTableProgram = runner.getSqlite3Builder()
                .arg(sqliteDbFile.getAbsolutePath())
                .arg(createTableSql)
                .build();
        ProcessResult<String, String> createTableResult = Sqlite3Runner.executeOrPropagateInterruption(createTableProgram, null);
        if (createTableResult.exitCode() != 0) {
            System.err.println(createTableResult.content().stderr());
            throw new SQLException("nonzero exit from sqlite3 " + createTableResult.exitCode());
        }
        return sqliteDbFile;
    }

    private void populateSampleDb(Sqlite3Runner.Sqlite3GenericImporter importer, Map<Integer, String> idToValue, File sqliteDbFile) throws IOException, SQLException {
        List<Map<String, String>> rows = new ArrayList<>(idToValue.size());
        idToValue.forEach((id, value) -> rows.add(ImmutableMap.of("id", id.toString(), "value", value)));
        importer.doImportRows(rows, Arrays.asList("id", "value"), sqliteDbFile, SAMPLE_DB_TABLE_NAME, tmp.getRoot().toPath(), "");
    }
}