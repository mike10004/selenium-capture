package io.github.mike10004.seleniumcapture.firefox;

import io.github.mike10004.seleniumcapture.ExecutableConfig;
import io.github.mike10004.seleniumcapture.Subprocesses;
import com.google.common.collect.ImmutableMap;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.Subprocess;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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
        return Sqlite3Runner.createDefaultSqlite3Config();
    }

    /**
     * Tests that we correctly escape a token inside an sqlite meta-command.
     * I'm not sure what the escaping requirements are exactly, so we take our best guess
     * and test it by actually doing an import from a csv file.
     */
    @Test
    public void escapeSqlite3Token() throws Exception {
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
            Sqlite3GenericImporter importer = new Sqlite3GenericImporter();
            populateSampleDb(importer, idToValue, sqliteDbFile);
            Sqlite3GenericExporter exporter = new Sqlite3GenericExporter(createRunner());
            List<Map<String, String>> rows = exporter.dumpRows(SAMPLE_DB_TABLE_NAME, sqliteDbFile);
            // System.out.format("rows: %s%n", rows);
            Map<Integer, String> exported = rows.stream().collect(Collectors.toMap(row -> Integer.valueOf(row.get("id")), row -> row.get("value")));
            assertEquals("exported", idToValue, exported);
        }
    }

    private static final String SAMPLE_DB_TABLE_NAME = "foo";
    private static final String createTableSql = "CREATE TABLE " + SAMPLE_DB_TABLE_NAME + " (" +
            "id INTEGER PRIMARY KEY, " +
            "value TEXT" +
            ");";

    @SuppressWarnings("UnusedReturnValue")
    private File createSampleDb(Sqlite3Runner runner, File sqliteDbFile) throws SQLException {
        Subprocess createTableProgram = runner.getSqlite3Builder()
                .arg(sqliteDbFile.getAbsolutePath())
                .arg(createTableSql)
                .build();
        ProcessResult<String, String> createTableResult = Subprocesses.executeOrPropagateInterruption(createTableProgram, Charset.defaultCharset(), null);
        if (createTableResult.exitCode() != 0) {
            System.err.println(createTableResult.content().stderr());
            throw new SQLException("nonzero exit from sqlite3 " + createTableResult.exitCode());
        }
        return sqliteDbFile;
    }

    private void populateSampleDb(Sqlite3GenericImporter importer, Map<Integer, String> idToValue, File sqliteDbFile) throws IOException, SQLException {
        List<Map<String, String>> rows = new ArrayList<>(idToValue.size());
        idToValue.forEach((id, value) -> rows.add(ImmutableMap.of("id", id.toString(), "value", value)));
        Sqlite3ImportInfo importInfo = Sqlite3ImportInfo.create(SAMPLE_DB_TABLE_NAME, Arrays.asList("id", "value"), createTableSql);
        importer.doImportRows(createRunner(), rows, importInfo, sqliteDbFile, tmp.getRoot().toPath());
    }
}