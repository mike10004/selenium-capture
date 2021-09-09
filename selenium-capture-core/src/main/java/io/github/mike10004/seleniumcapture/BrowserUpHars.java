package io.github.mike10004.seleniumcapture;

import com.browserup.harreader.HarReaderMode;
import com.browserup.harreader.model.Har;
import com.browserup.harreader.model.HarContent;
import com.browserup.harreader.model.HarEntry;
import com.browserup.harreader.model.HarLog;
import com.browserup.harreader.model.HarRequest;
import com.browserup.harreader.model.HarResponse;
import com.google.common.base.MoreObjects;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Static utility methods relating to HAR objects produced by BrowserUp Proxy.
 * See the {@code com.browserup.harreader} and {@code com.browserup.harreader.model}
 * packages.
 */
public class BrowserUpHars {

    private BrowserUpHars() {}

    /**
     * Serializes a HAR to a stream.
     * @param har har
     * @param writer stream
     * @throws IOException on I/O error
     */
    public static void writeHar(Har har, Writer writer) throws IOException {
        new CustomHarMapperFactory().instance(HarReaderMode.STRICT).writeValue(writer, har);
    }

    /**
     * Serializes a HAR to a file.
     * @param har har
     * @throws IOException on I/O error
     * @param file destination file
     * @param charset charset to encode text
     */
    public static void writeHar(Har har, File file, Charset charset) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), charset)) {
            writeHar(har, writer);
        }
    }

    /**
     * Deserializes a HAR from a file with lenient interpretation of content.
     * @param harFile the file
     * @param harCharset the character encoding of text in the file
     * @return the har
     * @throws IOException on I/O error
     * @see HarReaderMode#LAX
     */
    public static Har readHarTolerantly(File harFile, Charset harCharset) throws IOException {
        return readHar(harFile, harCharset, HarReaderMode.LAX);
    }

    /**
     * Deserializes a HAR from a character stream.
     * @param reader the character stream
     * @param mode reader mode
     * @return deserialized HAR
     * @throws IOException on I/O error
     */
    public static Har readHar(Reader reader, HarReaderMode mode) throws IOException {
        return new CustomHarMapperFactory().instance(mode).readValue(reader, Har.class);
    }

    /**
     * Deserializes a HAR from a file.
     * @param harFile the HAR file
     * @param harCharset the character encoding
     * @param mode reader mode
     * @return deserialized HAR
     * @throws IOException
     */
    public static Har readHar(File harFile, Charset harCharset, HarReaderMode mode) throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream(harFile), harCharset)) {
            return readHar(reader, mode);
        }
    }

    /**
     * Returns a short description of a HAR.
     * @param har har
     * @return string description
     */
    public static String describe(Har har) {
        if (har == null) {
            return "null";
        }
        return describe(har, (h, s) -> {
            s.add("log", describe(h.getLog()));
        });
    }

    /**
     * Returns a short description of a HAR log.
     * @param log log
     * @return string description
     */
    public static String describe(HarLog log) {
        if (log == null) {
            return "null";
        }
        List<HarEntry> entries = log.getEntries();
        Integer numEntries = entries == null ? null : entries.size();
        return describe(log, (l, h) -> {
            h.add("version", log.getVersion());
            h.add("entries.size", numEntries);
        });
    }

    /**
     * Returns a short description of a request.
     * @param request request
     * @return string description
     */
    public static String describe(HarRequest request) {
        if (request == null) {
            return "null";
        }
        return describe(request, (r, h) -> {
            h.add("url", r.getUrl())
             .add("method", r.getMethod());
        });
    }

    /**
     * Returns a short description of a content object.
     * @param content har
     * @return string description
     */
    public static String describe(HarContent content) {
        if (content == null) {
            return "null";
        }
        return describe(content, (r, h) -> {
            h.add("contentType", r.getMimeType())
             .add("encoding", r.getEncoding())
             .add("size", r.getSize());
        });
    }

    /**
     * Returns a short description of a response.
     * @param response response
     * @return string description
     */
    public static String describe(HarResponse response) {
        if (response == null) {
            return "null";
        }
        return describe(response, (r, h) -> {
            h.add("status", r.getStatus())
             .add("content", describe(r.getContent()));
        });
    }

    private static <T> String describe(T item, BiConsumer<T, MoreObjects.ToStringHelper> elaborator) {
        MoreObjects.ToStringHelper h = MoreObjects.toStringHelper(item.getClass());
        elaborator.accept(item, h);
        return h.toString();
    }
}
