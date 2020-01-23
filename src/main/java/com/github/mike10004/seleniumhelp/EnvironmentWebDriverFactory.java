package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableMap;
import org.openqa.selenium.MutableCapabilities;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

public abstract class EnvironmentWebDriverFactory implements WebDriverFactory {

    protected final Supplier<Map<String, String>> environmentSupplier;

    protected EnvironmentWebDriverFactory(Builder<?> builder) {
        this.environmentSupplier = checkNotNull(builder.environmentSupplier);
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

    static Supplier<Map<String, String>> createEnvironmentSupplierForDisplay(@Nullable String display) {
        if (display == null) {
            return ImmutableMap::of;
        } else {
            return () -> (ImmutableMap.of("DISPLAY", display));
        }
    }

    @SuppressWarnings("unchecked")
    public static abstract class Builder<B extends Builder> {
        private Supplier<Map<String, String>> environmentSupplier = HashMap::new;

        protected Builder() {
        }

        public final B environment(Supplier<Map<String, String>> environmentSupplier) {
            this.environmentSupplier = checkNotNull(environmentSupplier);
            return (B) this;
        }

        public final B environment(Map<String, String> environment) {
            this.environmentSupplier = () -> environment;
            return (B) this;
        }
    }

}
