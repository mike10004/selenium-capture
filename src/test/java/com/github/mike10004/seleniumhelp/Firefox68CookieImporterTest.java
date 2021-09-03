package com.github.mike10004.seleniumhelp;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class Firefox68CookieImporterTest {

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testImport() throws Exception {
        DeserializableCookie cookie = DeserializableCookie.builder("foo", "bar").domain("example.com").expiry(Instant.now().plus(Duration.ofHours(24))).build();
        Firefox68CookieImporter importer = new Firefox68CookieImporter(Sqlite3Runner.createDefault(), new Sqlite3GenericImporter());
        File sqliteDbFile = temporaryFolder.newFile();
        importer.importCookies(Collections.singleton(cookie), sqliteDbFile, temporaryFolder.getRoot().toPath());

        Sqlite3GenericExporter exporter = new Sqlite3GenericExporter(Sqlite3Runner.createDefault());
        List<Map<String, String>> rows = exporter.dumpRows("moz_cookies", sqliteDbFile);
        assertEquals("size", 1, rows.size());
        Map<String, String> row = rows.get(0);
        assertEquals("cookie name: " + row, "foo", row.get("name"));
        assertEquals("cookie value: " + row, "bar", row.get("value"));
    }
}

