package io.github.mike10004.seleniumcapture.testing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Class that represents the result of unzipping a file. Use {@link #unzip(File)} or
 * {@link #unzip(InputStream)} create an instance.
 */
public abstract class Unzippage {

    protected Unzippage() {}

    /**
     * Returns an iterable over the names of zip entries that represent compressed files.
     * @return the file entries
     */
    public abstract Iterable<String> fileEntries();

    /**
     * Returns an iterable over the names of zip entries that represent directories.
     * @return the directory entries
     */
    public abstract Iterable<String> directoryEntries();

    /**
     * Returns a byte source that supplies a stream containing the decompressed
     * bytes of a zip entry.
     * @param fileEntry the file entry
     * @return the byte source
     */
    public abstract ByteSource getFileBytes(String fileEntry);

    private static class CollectionUnzippage extends Unzippage {

        private final ImmutableList<String> directoryEntries;
        private final ImmutableMap<String, ByteSource> fileEntries;

        protected CollectionUnzippage(Iterable<String> directoryEntries, Map<String, ByteSource> fileEntries) {
            this.directoryEntries = ImmutableList.copyOf(directoryEntries);
            this.fileEntries = ImmutableMap.copyOf(fileEntries);
        }

        @Override
        public Iterable<String> fileEntries() {
            return fileEntries.keySet();
        }

        @Override
        public Iterable<String> directoryEntries() {
            return directoryEntries;
        }

        @Override
        public ByteSource getFileBytes(String fileEntry) {
            return fileEntries.get(fileEntry);
        }
    }

    /**
     * Unzips data from an input stream. The stream must be open and positioned
     * at the beginning of the zip data.
     * @param inputStream the input stream
     * @return the unzippage
     * @throws IOException if something goes awry
     */
    public static Unzippage unzip(InputStream inputStream) throws IOException {
        return unzip(new StreamZipFacade(inputStream));
    }

    private static class StreamZipFacade implements ZipFacade {

        private final ZipInputStream inputStream;

        private StreamZipFacade(InputStream inputStream) {
            this.inputStream = new ZipInputStream(inputStream);
        }

        private class StreamEntryFacade implements EntryFacade {

            private final ZipEntry entry;

            private StreamEntryFacade(ZipEntry entry) {
                this.entry = Objects.requireNonNull(entry);
            }

            @Override
            public InputStream openStream() throws IOException {
                return new FilterInputStream(inputStream) {
                    @Override
                    public void close() throws IOException {
                        inputStream.closeEntry();
                    }
                };
            }

            @Override
            public ZipEntry getEntry() {
                return entry;
            }
        }

        @Nullable
        @Override
        public EntryFacade next() throws IOException {
            ZipEntry entry = inputStream.getNextEntry();
            if (entry == null) {
                return null;
            }
            return new StreamEntryFacade(entry);
        }

    }

    /**
     * Unzips a zip file.
     * @param zipPathname the pathname of the zip file
     * @return the unzippage
     * @throws IOException if something goes awry
     */
    public static Unzippage unzip(File zipPathname) throws IOException {
        try (ZipFile zf = new ZipFile(zipPathname)) {
            return unzip(new FileZipFacade(zf));
        }
    }

    private interface ZipFacade {
        @Nullable
        EntryFacade next() throws IOException;
    }

    private interface EntryFacade {
        InputStream openStream() throws IOException;
        ZipEntry getEntry();
    }

    private static class FileZipFacade implements ZipFacade {

        private final ZipFile zipFile;
        private final Iterator<? extends ZipEntry> entries;

        public FileZipFacade(ZipFile zipFile) throws IOException {
            entries = zipFile.stream().iterator();
            this.zipFile = zipFile;
        }

        @Nullable
        @Override
        public EntryFacade next() throws IOException {
            if (entries.hasNext()) {
                ZipEntry entry = entries.next();
                return new EntryFacade() {
                    @Override
                    public ZipEntry getEntry() {
                        return entry;
                    }

                    @Override
                    public InputStream openStream() throws IOException {
                        return zipFile.getInputStream(entry);
                    }
                };
            } else {
                return null;
            }
        }

    }

    private static Unzippage unzip(ZipFacade entryProvider) throws IOException {
        List<String> directoryEntries = new ArrayList<>();
        Map<String, byte[]> fileEntries = new HashMap<>();
        EntryFacade session;
        while ((session = entryProvider.next()) != null) {
            ZipEntry entry = session.getEntry();
            if (entry.isDirectory()) {
                directoryEntries.add(entry.getName());
            } else {
                int bufferLen = Ints.checkedCast(Math.max(entry.getCompressedSize(), entry.getSize()));
                if (bufferLen <= 0) {
                    bufferLen = 256;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferLen);
                try (InputStream input = session.openStream()) {
                    ByteStreams.copy(input, baos);
                }
                baos.flush();
                fileEntries.put(entry.getName(), baos.toByteArray());
            }
        }
        return new CollectionUnzippage(directoryEntries, Maps.transformValues(fileEntries, ByteSource::wrap));
    }

}