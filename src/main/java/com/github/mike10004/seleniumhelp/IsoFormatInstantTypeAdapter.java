package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableList;
import com.google.gson.TypeAdapter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class IsoFormatInstantTypeAdapter extends FormattingInstantTypeAdapter {

    private static final DateTimeFormatter DEFAULT_OUTPUT_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private static final ImmutableList<DateTimeFormatter> DEFAULT_INPUT_PARSERS = ImmutableList.of(
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    private static final IsoFormatInstantTypeAdapter DEFAULT_INSTANCE = new IsoFormatInstantTypeAdapter();

    public IsoFormatInstantTypeAdapter() {
        super(DEFAULT_OUTPUT_FORMATTER, DEFAULT_INPUT_PARSERS);
    }

    /**
     * Returns the statically defined instance constructed with the default formatter.
     * @return the instance
     * @see #getDefaultOutputFormatter()
     * @see #getDefaultInputParsers()
     * @see FormattingInstantTypeAdapter#FormattingInstantTypeAdapter(DateTimeFormatter, Iterable)
     */
    public static TypeAdapter<Instant> getInstance() {
        return DEFAULT_INSTANCE;
    }

    public static ImmutableList<DateTimeFormatter> getDefaultInputParsers() {
        return DEFAULT_INPUT_PARSERS;
    }

    public static DateTimeFormatter getDefaultOutputFormatter() {
        return DEFAULT_OUTPUT_FORMATTER;
    }
}
