package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of a type adapter for Instant instances that tolerates
 * strings that represent Date instance as deserialization input.
 */
public class DateTolerantInstantTypeAdapter extends FormattingInstantTypeAdapter {

    private static final ImmutableList<DateTimeFormatter> DATE_TOLERANT_FORMATTERS = ImmutableList.of(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            // "Jun 7, 2019 2:07:27 PM"
            DateTimeFormatter.ofPattern("MMM d, uuuu h:mm:ss a z").withZone(ZoneId.of("UTC")),
            DateTimeFormatter.ofPattern("MMM d, uuuu h:mm:ss a O").withZone(ZoneId.of("UTC")),
            DateTimeFormatter.ofPattern("MMM d, uuuu h:mm:ss a X").withZone(ZoneId.of("UTC")),
            DateTimeFormatter.ofPattern("MMM d, uuuu h:mm:ss a").withZone(ZoneId.of("UTC")),
            DateTimeFormatter.ofPattern("MMM d, uuuu HH:mm:ss").withZone(ZoneId.of("UTC"))
    );
    private static final ImmutableList<DateTimeFormatter> INPUT_PARSERS = ImmutableList.copyOf(Iterables.concat(IsoFormatInstantTypeAdapter.getDefaultInputParsers(), DATE_TOLERANT_FORMATTERS));

    public DateTolerantInstantTypeAdapter() {
        super(IsoFormatInstantTypeAdapter.getDefaultOutputFormatter(), getInputParsers());
    }

    public static Iterable<DateTimeFormatter> getInputParsers() {
        return INPUT_PARSERS;
    }

}
