package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class EnvironmentWebDriverFactory implements WebDriverFactory {

    protected final Supplier<Map<String, String>> environmentSupplier;

    protected EnvironmentWebDriverFactory(Builder<?> builder) {
        this.environmentSupplier = checkNotNull(builder.mergedEnvironment());
    }

    public Map<String, String> supplyEnvironment() {
        return environmentSupplier.get();
    }

    static Map<String, String> createEnvironmentForDisplay(@Nullable String display) {
        Map<String, String> env = new HashMap<>();
        setDisplayEnvironmentVariable(env, display);
        return env;
    }

    static void setDisplayEnvironmentVariable(Map<String, String> env, @Nullable String display) {
        if (display != null) {
            env.put("DISPLAY", display);
        }
    }

    private static class EmptyEnvironmentSupplier implements Supplier<Map<String, String>> {

        public EmptyEnvironmentSupplier() {}

        @Override
        public Map<String, String> get() {
            return ImmutableMap.of();
        }

        @Override
        public String toString() {
            return "EmptyEnvironment{}";
        }
    }

    static Supplier<Map<String, String>> createEnvironmentSupplierForDisplay(@Nullable String display) {
        if (display == null) {
            return new EmptyEnvironmentSupplier();
        } else {
            return new Supplier<Map<String, String>>() {
                @Override
                public Map<String, String> get() {
                    return ImmutableMap.of("DISPLAY", display);
                }

                @Override
                public String toString() {
                    return String.format("Environment{DISPLAY=%s}", StringUtils.abbreviate(display, 16));
                }
            };
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static abstract class Builder<B extends Builder> {

        private final Map<String, String> hiddenEnvironment;
        private Supplier<Map<String, String>> environmentSupplier = HashMap::new;

        protected Builder() {
            hiddenEnvironment = new LinkedHashMap<>();
        }

        @SuppressWarnings("SameParameterValue")
        protected void hiddenEnvironmentVariable(String name, @Nullable String value) {
            if (value == null) {
                hiddenEnvironment.remove(name);
            } else {
                hiddenEnvironment.put(name, value);
            }
        }

        public final B environment(Supplier<Map<String, String>> environmentSupplier) {
            this.environmentSupplier = checkNotNull(environmentSupplier);
            return (B) this;
        }

        public final B environment(Map<String, String> environment) {
            this.environmentSupplier = Suppliers.ofInstance(environment);
            return (B) this;
        }

        private Supplier<Map<String, String>> mergedEnvironment() {
            return new Supplier<Map<String, String>>() {
                @Override
                public Map<String, String> get() {
                    Map<String, String> merged = new LinkedHashMap<>(hiddenEnvironment);
                    merged.putAll(environmentSupplier.get());
                    return merged;
                }

                @Override
                public String toString() {
                    return MoreObjects.toStringHelper("MergedEnvironment")
                            .add("hidden", hiddenEnvironment)
                            .add("overrides", environmentSupplier)
                            .toString();
                }
            };
        }
    }

}
