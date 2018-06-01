package com.github.mike10004.seleniumhelp;

import com.google.common.base.CharMatcher;
import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.helger.css.ECSSVersion;
import com.helger.css.reader.CSSReader;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarContent;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import net.lightbody.bmp.core.har.HarResponse;
import org.brotli.dec.BrotliInputStream;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service class that cleans a HAR by decoding responses that were Brotli-encoded.
 */
public class BrotliResponseTransform {

    private static final CharMatcher ALPHANUMERIC = CharMatcher.inRange('A', 'Z').or(CharMatcher.inRange('a', 'z')).or(CharMatcher.inRange('0', '9'));
    private static final CharMatcher BASE_64_ALPHABET = ALPHANUMERIC.or(CharMatcher.anyOf("/+="));

    static final String HEADER_VALUE_BROTLI_ENCODED = "br";

    public BrotliResponseTransform() {
    }

    public HarPostProcessor asPostProcessor() {
        return this::clean;
    }

    /**
     * Cleans a given har by decoding its responses where necessary.
     * @param inputHar the har to clean
     * @return a list of the modified entries
     */
    public List<HarEntry> clean(Har inputHar) {
        List<HarEntry> entries = inputHar.getLog().getEntries().stream().filter(entry -> {
            HarResponse rsp = entry.getResponse();
            HarContent content = rsp.getContent();
            String contentEncoding = getHeaderValue(rsp.getHeaders(), HttpHeaders.CONTENT_ENCODING);
            if (HEADER_VALUE_BROTLI_ENCODED.equalsIgnoreCase(contentEncoding) && content != null) {
                String text = content.getText();
                if (text != null && base64Alphabet().matchesAllOf(text)) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        clean(entries.stream());
        return entries;
    }

    private static boolean isContentType(Predicate<? super MediaType> predicate, @Nullable String contentType) {
        if (contentType != null && !"".equals(contentType.trim())) {
            MediaType mediaType = MediaType.parse(contentType.trim());
            return predicate.test(mediaType);
        }
        return false;
    }

    private static boolean isJavascriptContentType(@Nullable String contentType) {
        return isContentType(mediaType -> "javascript".equalsIgnoreCase(mediaType.subtype()), contentType);
    }

    protected void cleaningExceptionThrown(HarResponse response, Exception exception) {

    }

    protected void clean(Stream<HarEntry> entries) {
        entries.map(HarEntry::getResponse).forEach(response -> {
            try {
                clean(response);
            } catch (IOException e) {
                cleaningExceptionThrown(response, e);
            }
        });
    }

    protected Charset divineCharset(HarResponse response) {
        return StandardCharsets.UTF_8;
    }

    static class DecodedContentException extends IOException {
        public DecodedContentException(String message) {
            super(message);
        }

        @SuppressWarnings("unused")
        public DecodedContentException(String message, Throwable cause) {
            super(message, cause);
        }

        @SuppressWarnings("unused")
        public DecodedContentException(Throwable cause) {
            super(cause);
        }
    }

    static byte[] decompressBrotli(byte[] brotliCompressedData) throws IOException {
        byte[] decompressed;
        try (InputStream in = new BrotliInputStream(new ByteArrayInputStream(brotliCompressedData))) {
            decompressed = ByteStreams.toByteArray(in);
        }
        return decompressed;
    }

    protected void clean(HarResponse response) throws IOException {
        HarContent content = response.getContent();
        byte[] brotliBytes = java.util.Base64.getDecoder().decode(content.getText());
        byte[] decompressed = decompressBrotli(brotliBytes);
        String text, encoding;
        if (isJavascriptContentType(content.getMimeType())) {
            text = new String(decompressed, divineCharset(response));
            encoding = null;
            if (!isValidJavascript(text)) {
                throw new DecodedContentException("not valid javascript");
            }
        } else if (isContentType(m -> m.is(MediaType.CSS_UTF_8.withoutParameters()), content.getMimeType())) {
            text = new String(decompressed, divineCharset(response));
            encoding = null;
            if (!isValidCss(text)) {
                throw new DecodedContentException("not valid css");
            }
        } else {
            text = Base64.getEncoder().encodeToString(decompressed);
            encoding = "base64";
        }
        content.setEncoding(encoding);
        content.setText(text);
    }

    protected boolean isValidCss(String sourceCode) {
        for (ECSSVersion cssVersion : ECSSVersion.values()) {
            if (CSSReader.isValidCSS(sourceCode, cssVersion)) {
                return true;
            }
        }
        return false;
    }

    private static CharMatcher base64Alphabet() {
        return BASE_64_ALPHABET;
    }

    @SuppressWarnings("SameParameterValue")
    @Nullable
    private static String getHeaderValue(Iterable<HarNameValuePair> headers, String headerName) {
        for (HarNameValuePair header : headers) {
            if (headerName.equalsIgnoreCase(header.getName())) {
                return header.getValue();
            }
        }
        return null;
    }

    /**
     * Checks whether a string is parseable javascript source code.
     * Source: https://stackoverflow.com/a/24306346/2657036.
     * @param sourceCode the source code
     * @return true if it is valid javascript
     */
    protected boolean isValidJavascript(String sourceCode) {
        jdk.nashorn.internal.runtime.options.Options options = new jdk.nashorn.internal.runtime.options.Options("nashorn");
        options.set("anon.functions", true);
        options.set("parse.only", true);
        options.set("scripting", true);
        jdk.nashorn.internal.runtime.ErrorManager errors = new jdk.nashorn.internal.runtime.ErrorManager();
        jdk.nashorn.internal.runtime.Context context = new jdk.nashorn.internal.runtime.Context(options, errors, Thread.currentThread().getContextClassLoader());
        jdk.nashorn.internal.runtime.Source source   =  jdk.nashorn.internal.runtime.Source.sourceFor("test", sourceCode);
        jdk.nashorn.internal.parser.Parser parser = new jdk.nashorn.internal.parser.Parser(context.getEnv(), source, errors);
        parser.parse();
        return !errors.hasErrors();
    }
}
