package com.github.mike10004.seleniumhelp;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

class Csvs {

    private Csvs() {}

    public interface HeaderStrategy {
        /**
         * Interface to enable arbitrary production of an array of column headers.
         * @param readerBeforeFirstRead the CSV reader before the first {@link CSVReader#readNext()} call
         * @return an array of column headers, or null if reading CSV should be stopped
         * @throws IOException
         */
        @Nullable String[] produceHeaders(CSVReader readerBeforeFirstRead) throws IOException;
    }

    private static final HeaderStrategy headersFromFirstRowStrategy = new HeaderStrategy() {
        @Override
        public String[] produceHeaders(CSVReader readerBeforeFirstRead) throws IOException {
            return readerBeforeFirstRead.readNext();
        }
    };

    public static HeaderStrategy headersFromFirstRow() {
        return headersFromFirstRowStrategy;
    }

    public static HeaderStrategy staticHeaders(final Iterable<String> headers) {
        checkNotNull(headers);
        return new HeaderStrategy() {
            @Override
            public String[] produceHeaders(CSVReader readerBeforeFirstRead) throws IOException {
                return Iterables.toArray(headers, String.class);
            }
        };
    }

    public static List<Map<String, String>> readRowMaps(CharSource source, HeaderStrategy headerStrategy) throws IOException {
        String[] headers = null;
        List<Map<String, String>> rows = new ArrayList<>();
        try (CSVReader reader = new CSVReader(source.openStream())) {
            String[] row;
            while (true) {
                if (headers == null) {
                    headers = headerStrategy.produceHeaders(reader);
                    if (headers == null) {
                        break;
                    }
                } else {
                    row = reader.readNext();
                    if (row == null) {
                        break;
                    }
                    checkState(row.length == headers.length, "incongruent row length %d (%d headers)", row.length, headers.length);
                    Map<String, String> rowMap = new LinkedHashMap<>(row.length);
                    for (int i = 0;i < row.length;i ++) {
                        rowMap.put(headers[i], row[i]);
                    }
                    rows.add(rowMap);
                }
            }
        }
        return rows;
    }

    public enum UnknownKeyStrategy {
        IGNORE, FAIL
    }

    static class UnknownKeyException extends IllegalArgumentException {
        public UnknownKeyException(String key) {
            super(StringUtils.abbreviate(key, 128));
        }
    }

    public static String[] makeRowFromMap(List<String> headers, Map<String, String> map, String defaultValue, UnknownKeyStrategy unknownKeyStrategy) {
        checkNotNull(unknownKeyStrategy, "unknownKeyStrategy");
        String[] row = new String[headers.size()];
        Arrays.fill(row, defaultValue);
        for (String key : map.keySet()) {
            int index = headers.indexOf(key);
            if (index >= 0) {
                Object value = map.get(key);
                @Nullable String valueStr = value == null ? null : value.toString();
                if (valueStr != null) {
                    row[index] = valueStr;
                }
            } else {
                if (unknownKeyStrategy == UnknownKeyStrategy.FAIL) {
                    throw new UnknownKeyException(key);
                } else if (unknownKeyStrategy == UnknownKeyStrategy.IGNORE) {
                    // yup, just ignore it
                } else {
                    throw new IllegalStateException("bug: unhandled enum constant " + unknownKeyStrategy);
                }
            }
        }
        return row;
    }

    public static int writeRowMapsWithHeaders(Iterable<String> headers, Iterable<Map<String, String>> rows, String defaultValue, UnknownKeyStrategy unknownKeyStrategy, CharSink sink) throws IOException {
        return writeRowMaps(headers, true, rows, defaultValue, unknownKeyStrategy, sink);
    }

    public static int writeRowMaps(Iterable<String> headers, Iterable<Map<String, String>> rows, String defaultValue, UnknownKeyStrategy unknownKeyStrategy, CharSink sink) throws IOException {
        return writeRowMaps(headers, false, rows, defaultValue, unknownKeyStrategy, sink);
    }

    private static int writeRowMaps(Iterable<String> headers, boolean includeHeaders,
                                      Iterable<Map<String, String>> rows, String defaultValue, UnknownKeyStrategy unknownKeyStrategy, CharSink sink) throws IOException {
        List<String> headersList = ImmutableList.copyOf(headers);
        int numOutputRows = 0;
        try (CSVWriter out = new CSVWriter(sink.openStream())) {
            if (includeHeaders) {
                out.writeNext(Iterables.toArray(headers, String.class));
            }
            numOutputRows++;
            for (Map<String, String> rowInput : rows) {
                String[] rowOutput = makeRowFromMap(headersList, rowInput, defaultValue, unknownKeyStrategy);
                out.writeNext(rowOutput);
                numOutputRows++;
            }
        }
        return numOutputRows;
    }

    private static class CharBucket extends CharSink {

        private final StringWriter sw;

        public CharBucket(int initialSize) {
            this(new StringWriter(initialSize));
        }

        public CharBucket(StringWriter sw) {
            this.sw = checkNotNull(sw);
        }

        @Override
        public Writer openStream() throws IOException {
            return sw;
        }

        @Override
        public String toString() {
            return sw.toString();
        }
    }

    public static String writeRowMapsWithHeadersToString(Iterable<String> headers, Iterable<Map<String, String>> rows, String defaultValue, UnknownKeyStrategy unknownKeyStrategy) throws IOException {
        return writeRowMapsToString(headers, true, rows, defaultValue, unknownKeyStrategy);
    }

    public static String writeRowMapsToString(Iterable<String> headers, Iterable<Map<String, String>> rows, String defaultValue, UnknownKeyStrategy unknownKeyStrategy) throws IOException {
        return writeRowMapsToString(headers, false, rows, defaultValue, unknownKeyStrategy);
    }

    private static String writeRowMapsToString(Iterable<String> headers, boolean includeHeaders, Iterable<Map<String, String>> rows, String defaultValue, UnknownKeyStrategy unknownKeyStrategy) throws IOException {
        CharBucket bucket = new CharBucket(256);
        writeRowMaps(headers, includeHeaders, rows, defaultValue, unknownKeyStrategy, bucket);
        return bucket.toString();
    }

    /**
     * Creates and returns a converter using the given column names, ignoring unknown keys and using the empty string
     * as the default value.
     * @param columnNames the column names
     * @return the converter
     */
    public Converter<String[], Map<String, String>> rowToMapConverter(Iterable<String> columnNames) {
        return rowToMapConverter(columnNames, UnknownKeyStrategy.IGNORE, "");
    }

    public Converter<String[], Map<String, String>> rowToMapConverter(Iterable<String> columnNames, UnknownKeyStrategy unknownKeyStrategy, @Nullable String defaultColumnValue) {
        return new RowToMapConverter(columnNames, unknownKeyStrategy, defaultColumnValue);
    }

    static class RowToMapConverter extends Converter<String[], Map<String, String>> {

        private final ImmutableList<String> columnNames;
        private final String defaultColumnValue;
        private final UnknownKeyStrategy unknownKeyStrategy;

        public RowToMapConverter(Iterable<String> columnNames, UnknownKeyStrategy unknownKeyStrategy, @Nullable String defaultColumnValue) {
            this.columnNames = ImmutableList.copyOf(columnNames);
            this.unknownKeyStrategy = checkNotNull(unknownKeyStrategy);
            this.defaultColumnValue = defaultColumnValue;
        }

        @Override
        protected ImmutableMap<String, String> doForward(String[] row) {
            checkArgument(row.length == columnNames.size(), "row incongruent with known headers: %d != %d (%s)", row.length, columnNames.size(), columnNames);
            ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
            for (int i = 0; i < row.length; i++) {
                map.put(columnNames.get(i), row[i]);
            }
            return map.build();
        }

        @Override
        protected String[] doBackward(Map<String, String> map) {
            return Csvs.makeRowFromMap(columnNames, map, defaultColumnValue, unknownKeyStrategy);
        }

    }

}
