package io.github.mike10004.seleniumcapture;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.CapabilityType;

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

    public final B acceptInsecureCerts() {
        return acceptInsecureCerts(true);
    }

    public final B acceptInsecureCerts(boolean acceptInsecureCerts) {
        return configure(o -> o.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, acceptInsecureCerts));
    }

}
