package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class DateTolerantInstantTypeAdapterTest {

    @Test
    public void deserialize() throws Exception {
        List<Triple<String, Instant, Instant>> failures = new ArrayList<>();
        String ampmNoTzDateStr = "Jun 25, 2019 12:07:27 PM";
        String ampmDateStr = "Jun 25, 2019 12:07:27 PM EDT";
        List<Pair<String, Instant>> inputs = Arrays.asList(
                Pair.of(ampmNoTzDateStr, newDateFormat("MMM dd, yyyy hh:mm:ss a", TimeZone.getTimeZone("UTC")).parse(ampmNoTzDateStr).toInstant()),
                Pair.of(ampmDateStr, newDateFormat("MMM dd, yyyy hh:mm:ss a z", TimeZone.getTimeZone("UTC")).parse(ampmDateStr).toInstant())
        );
        for (Pair<String, Instant> input : inputs) {
            String dateStr = input.getLeft();
            Instant expected = input.getRight();
            Instant actual = new DateTolerantInstantTypeAdapter().parse(dateStr, callback);
            if (!Objects.equals(expected, actual)) {
                failures.add(Triple.of(dateStr, expected, actual));
            }
        }
        failures.forEach(triple -> {
            String inputStr = triple.getLeft();
            System.out.format("input \"%s\" -> %s (expected %s)%n", StringEscapeUtils.escapeJava(inputStr), triple.getRight(), triple.getMiddle());
        });
        assertEquals("deserialized", ImmutableList.of(), failures);
    }

    private static final Consumer<DateTimeFormatter> callback = parser -> {
        System.out.format("parsed with %s%n", parser);
    };

    private static DateFormat newDateFormat(String pattern, TimeZone tz) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        if (tz != null) {
            format.setTimeZone(tz);
        }
        return format;
    }
}