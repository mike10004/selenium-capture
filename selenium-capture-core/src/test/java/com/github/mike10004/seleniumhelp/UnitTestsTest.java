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

    @Test
    public void getChromeOptionsExtraArgs() {
        String[][] testCases = {
                {null, "null"},
                {"", "empty string"},
                {" ", "single space char"},
                {"--no-sandbox", "one arg", "--no-sandbox"},
                {"--no-sandbox ", "trailing whitespace", "--no-sandbox"},
                {" --no-sandbox", "leading whitespace", "--no-sandbox"},
                {"--no-sandbox --disable-gpu", "two args", "--no-sandbox", "--disable-gpu"},
                {"--no-sandbox --proxy=1.2.3.4:5678", "two args with value", "--no-sandbox", "--proxy=1.2.3.4:5678"},
        };
        Stream.of(testCases).forEach(testCase -> {
            String input = testCase[0];
            String message = testCase[1];
            List<String> expected = Arrays.asList(testCase).subList(2, testCase.length);
            List<String> actual = UnitTests.getChromeOptionsExtraArgs(input);
            assertEquals(message, expected, actual);
        });

    }

    @Test
    public void getIniSetting() throws Exception {
        Ini ini = new Ini();
//        org.ini4j.Config c = new Config();
//        c.setEmptyOption(true);
//        ini.setConfig(c);
        ini.load(new StringReader("foo = bar\n" +
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
