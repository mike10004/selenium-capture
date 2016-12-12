package com.github.mike10004.seleniumhelp;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class EnvironmentWebDriverFactory implements WebDriverFactory {

    protected final Supplier<Map<String, String>> environmentSupplier;

    public EnvironmentWebDriverFactory(Map<String, String> environment) {
        this(Suppliers.ofInstance(ImmutableMap.copyOf(environment)));
    }

    public EnvironmentWebDriverFactory() {
        this(createEnvironmentSupplierForDisplay(null));
    }

    public EnvironmentWebDriverFactory(Supplier<Map<String, String>> environmentSupplier) {
        this.environmentSupplier = checkNotNull(environmentSupplier, "environmentSupplier");
    }

    static Supplier<Map<String, String>> createEnvironmentSupplierForDisplay(@Nullable String display) {
        if (display == null) {
            return Suppliers.ofInstance(ImmutableMap.of());
        } else {
            return Suppliers.ofInstance(ImmutableMap.of("DISPLAY", display));
        }
    }

}
