package io.github.mike10004.seleniumcapture;

import com.google.common.collect.ImmutableList;
import org.openqa.selenium.MutableCapabilities;

import java.util.function.Consumer;

public abstract class CapableWebDriverFactory<C extends MutableCapabilities> extends EnvironmentWebDriverFactory {

    private final ImmutableList<Consumer<? super C>> optionsModifiers;

    protected CapableWebDriverFactory(CapableWebDriverFactoryBuilder<?, C> builder) {
        super(builder);
        optionsModifiers = ImmutableList.copyOf(builder.configurators);
    }

    protected void modifyOptions(C options) {
        for (Consumer<? super C> modifier : optionsModifiers) {
            modifier.accept(options);
        }
    }

}
