package com.github.mike10004.seleniumhelp;

import io.github.mike10004.nitsick.LayeredSettingSet;
import io.github.mike10004.nitsick.SettingLayer;
import io.github.mike10004.nitsick.SettingSet;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.annotation.Nullable;
import java.io.File;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class IniSettingLayerTest {

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String text = "foo = bar\n" +
            "eenie = meanie\n" +
            "a.b = c\n" +
            "[section1]\n" +
            "foo = baz\n" +
            "gaw = haw\n" +
            "d.e = f\n";

    @Test
    public void fromFile_absent() throws Exception {
        SettingLayer layer = IniSettingLayer.fromFile(temporaryFolder.newFile(), StandardCharsets.UTF_8);
        assertNull(layer.apply("foo"));
    }

    @Test
    public void fromFile_badPath() throws Exception {
        // expect input read error
        try {
            IniSettingLayer.fromFile(temporaryFolder.newFolder(), StandardCharsets.UTF_8).apply("anything");
            fail("it loaded");
        } catch (UncheckedIOException ignore) {
            // pass
        }
    }

    @Test
    public void fromFile_noSection() throws Exception {
        fromFile(null, "bar", "meanie", IniSettingLayer.GlobalSection.USE_FOR_DEFAULTS);
    }

    @Test
    public void fromFile_section() throws Exception {
        fromFile("section1", "baz", "meanie", IniSettingLayer.GlobalSection.USE_FOR_DEFAULTS);
    }

    @Test
    public void fromFile_section_nodefault() throws Exception {
        SettingLayer l = fromFile("section1", "baz", null, IniSettingLayer.GlobalSection.IGNORE);
        assertNull(l.apply("section1.eenie"));
    }

    public SettingLayer fromFile(@Nullable String section, String expectedFooValue, String expectedEenieValue, IniSettingLayer.GlobalSection globalSection) throws Exception {
        String prefix = section == null ? "" : section + ".";
        File iniFile = temporaryFolder.newFile();
        Charset cs = StandardCharsets.UTF_8;
        java.nio.file.Files.write(iniFile.toPath(), text.getBytes(cs));
        SettingLayer layer = section == null
                ? IniSettingLayer.fromFile(iniFile, cs)
                : IniSettingLayer.fromFile(iniFile, cs, section, globalSection);
        assertEquals("foo", expectedFooValue, layer.apply(prefix + "foo"));
        assertEquals("eenie (global)", expectedEenieValue, layer.apply(prefix + "eenie"));
        if (section != null) {
            assertNull(layer.apply("foo"));
        }
        return layer;
    }

    @Test
    public void settingSet() throws Exception {
        SettingLayer layer = IniSettingLayer.fromText(text, "section1",
                IniSettingLayer.GlobalSection.USE_FOR_DEFAULTS);
        SettingSet settings = LayeredSettingSet.of("section1", layer);
        assertEquals("foo (section overrides global)", "baz", settings.get("foo"));
        assertEquals("eenie (global)", "meanie", settings.get("eenie"));
        assertNull(settings.get("section1.foo"));
        assertNull(settings.get("section1.gaw"));
        assertNull(settings.get("section1.eenie"));
        assertEquals("a.b", "c", settings.get("a.b"));
        assertEquals("d.e", "f", settings.get("d.e"));
    }

}