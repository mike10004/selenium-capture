package com.github.mike10004.seleniumhelp;

import org.openqa.selenium.MutableCapabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("unchecked")
public abstract class CapableWebDriverFactoryBuilder<B extends EnvironmentWebDriverFactory.Builder, C extends MutableCapabilities> extends EnvironmentWebDriverFactory.Builder<B> {

    final List<Consumer<? super C>> configurators = new ArrayList<>();

    protected CapableWebDriverFactoryBuilder() {
        super();
    }

    public final B configure(Consumer<? super C> configurator) {
        configurators.add(requireNonNull(configurator));
        return (B) this;
    }
}
