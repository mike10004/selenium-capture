package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import org.apache.commons.io.input.ReaderInputStream;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("unused")
public class ImmutableHttpMessage {

    public static final Charset DEFAULT_HTTP_CHARSET = StandardCharsets.ISO_8859_1;

    private final HttpContentSource dataSource;
    public final ImmutableMultimap<String, String> headers;

    protected ImmutableHttpMessage(MessageBuilder<?> builder) {
        dataSource = builder.dataSource;
        headers = ImmutableMultimap.copyOf(builder.headers);
    }

    protected MoreObjects.ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("contentSource", dataSource)
                .add("headers.size", headers.size());
    }

    public ByteSource getContentAsBytes() {
        return dataSource.asBytes(this);
    }

    public CharSource getContentAsChars() {
        return dataSource.asChars(this);
    }

    protected static CharSource encodeToBase64(ByteSource byteSource) {
        return new CharSource() {
            @Override
            public Reader openStream() throws IOException {
                return new StringReader(Base64.getEncoder().encodeToString(byteSource.read()));
            }
        };
    }

    protected static ByteSource encodeToBytes(CharSource charSource, Supplier<Charset> charsetSupplier) {
        return new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
                return new ReaderInputStream(charSource.openStream(), charsetSupplier.get());
            }

            @Override
            public InputStream openBufferedStream() throws IOException {
                return new ReaderInputStream(charSource.openBufferedStream(), charsetSupplier.get());
            }
        };
    }

    protected static class Base64ByteSource extends ByteSource {

        private final String base64Data;
        private final Supplier<Decoder> decoderSupplier;

        public static Base64ByteSource forBase64String(String base64Data) {
            return new Base64ByteSource(base64Data, Base64::getDecoder);
        }

        private Base64ByteSource(String base64Data, Supplier<Decoder> decoderSupplier) {
            this.base64Data = checkNotNull(base64Data);
            this.decoderSupplier = checkNotNull(decoderSupplier);
        }

        @Override
        public InputStream openStream() throws IOException {
            return decoderSupplier.get().wrap(new ReaderInputStream(new StringReader(base64Data), StandardCharsets.US_ASCII));
        }
    }

    public interface HttpContentSource {

        boolean isNativelyText();
        CharSource asChars(ImmutableHttpMessage message);
        ByteSource asBytes(ImmutableHttpMessage message);

        static HttpContentSource empty() {
            return new HttpContentSource() {
                @Override
                public boolean isNativelyText() {
                    return false;
                }

                @Override
                public CharSource asChars(ImmutableHttpMessage message) {
                    return CharSource.empty();
                }

                @Override
                public ByteSource asBytes(ImmutableHttpMessage message) {
                    return ByteSource.empty();
                }

                @Override
                public String toString() {
                    return "HttpContentSource{empty}";
                }
            };
        }

        static HttpContentSource fromBytes(ByteSource byteSource) {
            return new OriginalByteSource(byteSource);
        }

        static HttpContentSource fromChars(CharSource charSource) {
            return new OriginalCharSource(charSource);
        }

        static HttpContentSource fromBase64(String base64Data) {
            return new OriginalByteSource(Base64ByteSource.forBase64String(base64Data));
        }
    }

    protected static final class OriginalByteSource implements HttpContentSource {

        private final ByteSource byteSource;

        public OriginalByteSource(ByteSource byteSource) {
            this.byteSource = checkNotNull(byteSource);
        }

        @Override
        public CharSource asChars(ImmutableHttpMessage message) {
            return encodeToBase64(byteSource);
        }

        @Override
        public ByteSource asBytes(ImmutableHttpMessage message) {
            return byteSource;
        }

        @Override
        public boolean isNativelyText() {
            return false;
        }

        @Override
        public String toString() {
            return "OriginalByteSource{" +
                    "nativelyText=" + isNativelyText() +
                    ",byteSource.size=" + byteSource.sizeIfKnown() +
                    '}';
        }
    }

    protected static final class OriginalCharSource implements HttpContentSource {

        private final CharSource charSource;

        public OriginalCharSource(CharSource charSource) {
            this.charSource = checkNotNull(charSource);
        }

        @Override
        public boolean isNativelyText() {
            return true;
        }

        @Override
        public CharSource asChars(ImmutableHttpMessage message) {
            return charSource;
        }

        @Override
        public ByteSource asBytes(ImmutableHttpMessage message) {
            MediaType contentType = message.getContentType();
            final Charset charset;
            if (contentType != null && contentType.charset().isPresent()) {
                charset = contentType.charset().get();
            } else {
                charset = DEFAULT_HTTP_CHARSET;
            }
            return encodeToBytes(charSource, () -> charset);
        }

        @Override
        public String toString() {
            return "OriginalCharSource{" +
                    "nativelyText=" + isNativelyText() +
                    ",charSource.length=" + charSource.lengthIfKnown() +
                    '}';
        }
    }

    /**
     * Finds headers by name, case-insensitively.
     * @param headerName header name
     * @return the headers
     */
    public Stream<Entry<String, String>> getHeaders(String headerName) {
        checkNotNull(headerName);
        return headers.entries().stream().filter(entry -> entry.getKey().equalsIgnoreCase(headerName));
    }

    public Stream<String> getHeaderValues(String headerName) {
        return getHeaders(headerName).map(Map.Entry::getValue);
    }

    @Nullable
    public MediaType getContentType() {
        String headerValue = getFirstHeaderValue(HttpHeaders.CONTENT_TYPE);
        if (headerValue == null) {
            return null;
        }
        return MediaType.parse(headerValue);
    }

    @Nullable
    public String getFirstHeaderValue(String headerName) {
        return getHeaders(headerName).map(Entry::getValue).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    public static abstract class MessageBuilder<B extends MessageBuilder> {

        private HttpContentSource dataSource = HttpContentSource.empty();
        private final Multimap<String, String> headers = ArrayListMultimap.create();

        protected MessageBuilder() {
        }

        public B content(HttpContentSource contentSource) {
            dataSource = checkNotNull(contentSource);
            return (B) this;
        }

        public B content(ByteSource byteSource) {
            dataSource = new OriginalByteSource(byteSource);
            return (B) this;
        }

        public B content(CharSource charSource) {
            dataSource = new OriginalCharSource(charSource);
            return (B) this;
        }

        public B headers(Multimap<String, String> val) {
            headers.clear();
            headers.putAll(val);
            return (B) this;
        }

        public B addHeaders(Collection<? extends Entry<String, String>> headers) {
            return addHeaders(headers.stream());
        }

        public B addHeaders(Stream<? extends Map.Entry<String, String>> headers) {
            headers.forEach(entry -> this.headers.put(entry.getKey(), entry.getValue()));
            return (B) this;
        }
    }
}
