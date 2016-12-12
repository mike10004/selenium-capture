package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CsvsTest {

    @Test
    public void readRowMaps_headersFromFirstRow() throws Exception {
        String text = "A,B,C\n" +
                "1,2,3\n" +
                "4,5,6\n";
        List<Map<String, String>> rowMaps = Csvs.readRowMaps(CharSource.wrap(text), Csvs.headersFromFirstRow());
        assertEquals("first data row", ImmutableMap.of("A", "1", "B", "2", "C", "3"), rowMaps.get(0));
        assertEquals("second data row", ImmutableMap.of("A", "4", "B", "5", "C", "6"), rowMaps.get(1));
        assertEquals("num rows", 2, rowMaps.size());
    }

    @Test
    public void makeRowFromMap_gardenPath() throws Exception {
        String[] row = Csvs.makeRowFromMap(ImmutableList.of("A", "B", "C"),
                ImmutableMap.of("A", "1", "B", "2", "C", "3"),
                "", Csvs.UnknownKeyStrategy.FAIL);
        assertArrayEquals("row", new String[]{"1", "2", "3"}, row);
    }

    @Test
    public void makeRowFromMap_replaceMissingWithDefault() throws Exception {
        String defaultVal = "x";
        String[] row = Csvs.makeRowFromMap(ImmutableList.of("A", "B", "C"),
                ImmutableMap.of("A", "1", "C", "3"),
                defaultVal, Csvs.UnknownKeyStrategy.FAIL);
        assertArrayEquals("row", new String[]{"1", defaultVal, "3"}, row);
    }

    @Test
    public void makeRowFromMap_ignoreUnknownKeys() throws Exception {
        String[] row = Csvs.makeRowFromMap(ImmutableList.of("A", "B", "C"),
                ImmutableMap.of("A", "1", "B", "2", "C", "3", "D", "4"),
                "*", Csvs.UnknownKeyStrategy.IGNORE);
        assertArrayEquals("row", new String[]{"1", "2", "3"}, row);
    }

    @Test(expected= Csvs.UnknownKeyException.class)
    public void makeRowFromMap_failOnUnknownKeys() throws Exception {
        Csvs.makeRowFromMap(ImmutableList.of("A", "B", "C"),
                ImmutableMap.of("A", "1", "B", "2", "C", "3", "D", "4"),
                "*", Csvs.UnknownKeyStrategy.FAIL);
    }

}