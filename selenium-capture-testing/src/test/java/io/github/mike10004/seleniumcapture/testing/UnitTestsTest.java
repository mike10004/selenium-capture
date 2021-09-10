package io.github.mike10004.seleniumcapture.testing;

import io.github.mike10004.seleniumcapture.testing.UnitTests;
import org.ini4j.Config;
import org.ini4j.Ini;
import org.junit.Test;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class UnitTestsTest {

    @org.junit.Ignore("not yet sure how ini impl works")
    @Test
    public void getIniSetting() throws Exception {
        Ini ini = new Ini();
        org.ini4j.Config c = new Config();
        c.setGlobalSection(true);
        c.setGlobalSectionName("default");
        ini.setConfig(c);
        ini.load(new StringReader("[default]\nfoo = bar\n" +
                "gaw = haw\n" +
                "\n" +
                "[section]\n" +
                "foo = baz\n" +
                "jay = kay\n"));
        Object globalFoo = ini.get("foo");
        assertEquals("global foo", "bar", globalFoo);
        String sectionFoo = ini.get("section", "foo");
        assertEquals("section foo", "baz", sectionFoo);
    }

    @Test
    public void resolveRepoRoot() throws Exception {
        Path repoDir = UnitTests.resolveRepoRoot();
        List<File> notFound = new ArrayList<>();
        for (String childName : new String[] {
                "selenium-capture-core",
                "selenium-capture-firefox",
                "selenium-capture-testing",
                "README.md",
                "LICENSE",
                "pom.xml",
        } ) {
            File expectedFile = repoDir.resolve(childName).toFile();
            if (!expectedFile.exists()) {
                notFound.add(expectedFile);
            }
        }
        assertEquals("expect children of repo root", Collections.emptyList(), notFound);
    }
}
