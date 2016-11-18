/*
 * (c) 2016 Novetta
 *
 * Created by mike
 */
package com.github.mike10004.seleniumhelp;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class OptionalDisplayWebDriverFactory implements WebDriverFactory {

    protected final Supplier<String> displayProvider;

    public OptionalDisplayWebDriverFactory() {
        this(Suppliers.ofInstance((String)null));
    }

    public OptionalDisplayWebDriverFactory(String display) {
        this(Suppliers.ofInstance(checkNotNull(display, "display")));
    }

    public OptionalDisplayWebDriverFactory(Supplier<String> displayProvider) {
        this.displayProvider = checkNotNull(displayProvider);
    }

    protected ImmutableMap<String, String> buildEnvironment() {
        String display = getDisplay();
        if (display == null) {
            return ImmutableMap.of();
        } else {
            return ImmutableMap.of("DISPLAY", display);
        }
    }

    protected @Nullable String getDisplay() {
        return displayProvider.get();
    }

}
