package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Ordering;
import io.github.mike10004.nitsick.SettingLayer;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.ini4j.Ini;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class IniSettingLayer implements SettingLayer {

    private final LazyInitializer<Function<String, String>> lazyIniText;

    private IniSettingLayer(Supplier<Function<String, String>> lazyIniText) {
        this(new LazyInitializer<Function<String, String>>() {
            @Override
            protected Function<String, String> initialize() {
                return lazyIniText.get();
            }
        });
        requireNonNull(lazyIniText, "lazyIniTextSupplier");
    }

    private IniSettingLayer(LazyInitializer<Function<String, String>> lazyIniText) {
        this.lazyIniText = requireNonNull(lazyIniText);
    }

    public static SettingLayer fromFile(File iniFile, Charset charset) {
        return fromSupplier(() -> readButAllowNotFound(iniFile, charset), null, GlobalSection.USE_FOR_DEFAULTS);
    }

    public static SettingLayer fromFile(File iniFile, Charset charset, String sectionName, GlobalSection globalSection) {
        requireNonNull(globalSection);
        requireNonNull(sectionName);
        return fromSupplier(() -> readButAllowNotFound(iniFile, charset), sectionName, globalSection);
    }

    private static SettingLayer fromSupplier(Supplier<String> iniTextSupplier, @Nullable String sectionName, GlobalSection globalSection) {
        if (sectionName == null && globalSection != GlobalSection.USE_FOR_DEFAULTS) {
            return k -> null;
        }
        return new IniSettingLayer(new Supplier<Function<String, String>>() {
            @Override
            public Function<String, String> get() {
                // supplied value is memoized by constructor
                return toFunction(new Ini4jShim(), iniTextSupplier.get(), sectionName, globalSection);
            }
        });
    }

    public static SettingLayer fromText(String iniText) {
        requireNonNull(iniText);
        return fromSupplier(() -> iniText, null, GlobalSection.USE_FOR_DEFAULTS);
    }

    public static SettingLayer fromText(String iniText, String section, GlobalSection globalSection) {
        requireNonNull(section);
        requireNonNull(globalSection);
        requireNonNull(iniText);
        return fromSupplier(() -> iniText, section, globalSection);
    }

    public enum GlobalSection {
        IGNORE,
        USE_FOR_DEFAULTS
    }

    interface IniShim<T> {

        T read(String iniText) throws IOException;

        boolean isGlobalSection(T ini, String sectionName);

        Set<String> getSections(T ini);

        String getValueInSection(T ini, String sectionName, String key);

        List<String> getKeysInSection(T ini, String sectionName);

    }

    private static class Ini4jShim implements IniShim<org.ini4j.Ini> {

        private static final String GLOBAL_SECTION_NAME = "_";

        public Ini4jShim() {
        }

        @Override
        public Ini read(String iniText) throws IOException {
            Ini ini = new Ini();
            ini.getConfig().setGlobalSection(true);
            ini.getConfig().setGlobalSectionName(GLOBAL_SECTION_NAME);
            iniText = "[" + GLOBAL_SECTION_NAME + "]\n" + iniText;
            ini.load(new StringReader(iniText));
            return ini;
        }

        @Override
        public boolean isGlobalSection(Ini ini, String sectionName) {
            return GLOBAL_SECTION_NAME.equals(sectionName);
        }

        @Override
        public Set<String> getSections(Ini ini) {
            return ini.keySet();
        }

        @Override
        public String getValueInSection(Ini ini, String sectionName, String key) {
            return ini.get(sectionName, key);
        }

        @Override
        public List<String> getKeysInSection(Ini ini, String sectionName) {
            return new ArrayList<>(ini.get(sectionName).keySet());
        }
    }

    /**
     * The way we transform an INI to a function is that we consider keys
     * inside sections to be prefixed by the section name plus dot
     * @param iniText
     * @param sectionNames
     * @param globalSection
     * @return
     */
    private static <T> Function<String, String> toFunction(IniShim<T> iniShim, String iniText, @Nullable String sectionName, GlobalSection globalSection) {
        T ini;
        try {
            ini = iniShim.read(iniText);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Set<String> sectionSet = iniShim.getSections(ini);
        Map<String, String> map = new HashMap<>();
        List<String> sections = new ArrayList<>(sectionSet);
        sections.sort(sectionOrdering());
        sections.forEach(section -> {
            if (iniShim.isGlobalSection(ini, section) && globalSection == GlobalSection.USE_FOR_DEFAULTS) {
                iniShim.getKeysInSection(ini, section).forEach(key -> {
                    String prefixedKey = sectionName == null ? key : sectionName + DOT + key;
                    String value = iniShim.getValueInSection(ini, section, key);
                    map.put(prefixedKey, value);
                });
            } else if (section != null && section.equals(sectionName)) {
                iniShim.getKeysInSection(ini, section).forEach(key -> {
                    String prefixedKey = sectionName + DOT + key;
                    String value = iniShim.getValueInSection(ini, section, key);
                    map.put(prefixedKey, value);
                });
            }

        });
        return new IniSearcher(map, globalSection).asFunction();
    }

    private static final String DOT = ".";

    /**
     * Returns an ordering that forces the global (key=null) section
     * first, so that is values for the defaults in the map that is created.
     * @return an ordering
     */
    private static Comparator<String> sectionOrdering() {
        return Ordering.natural().nullsFirst();
    }

    private static class IniSearcher {

        private final Map<String, String> map;
        private final GlobalSection globalSection;

        public IniSearcher(Map<String, String> map, GlobalSection globalSection) {
            this.map = map;
            this.globalSection = globalSection;
        }

        public Function<String, String> asFunction() {
            return new Function<String, String>() {
                @Override
                public String apply(String s) {
                    return getValue(s);
                }

                @Override
                public String toString() {
                    return MoreObjects.toStringHelper("IniSearcherFunction")
                            .add("map.size", map.size())
                            .add("global", globalSection)
                            .toString();
                }
            };
        }

        @Nullable
        public String getValue(String key) {
            return map.get(key);
        }
    }

    private static String readButAllowNotFound(File file, Charset charset) throws UncheckedIOException {
        try {
            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
            return new String(data, charset);
        } catch (FileNotFoundException | NoSuchFileException e) {
            return "";
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }



    @Override
    public String apply(String key) {
        try {
            return lazyIniText.get().apply(key);
        } catch (ConcurrentException e) {
            throw new IniLoadException(e);
        }
    }

    private static class IniLoadException extends RuntimeException {
        public IniLoadException(Throwable cause) {
            super(cause);
        }
    }

}

