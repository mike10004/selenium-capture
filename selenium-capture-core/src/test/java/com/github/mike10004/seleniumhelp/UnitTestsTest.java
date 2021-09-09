package com.github.mike10004.seleniumhelp;

import org.ini4j.Config;
import org.ini4j.Ini;
import org.junit.Test;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

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
}
