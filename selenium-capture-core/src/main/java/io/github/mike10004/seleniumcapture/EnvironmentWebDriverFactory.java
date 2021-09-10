package io.github.mike10004.seleniumcapture;

import com.google.common.base.MoreObjects;
import com.google.common.base.Suppliers;

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
