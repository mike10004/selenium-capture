package io.github.mike10004.seleniumcapture;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public interface HostBypassRuleFactory {

    static HostBypassRuleFactory createDefault() {
        return new DefaultHostBypassRuleFactory();
    }

    HostBypassRule fromSpec(String bypassSpec);

    default List<HostBypassRule> fromSpecs(Iterable<String> hostBypassSpec) {
        return StreamSupport.stream(hostBypassSpec.spliterator(), false)
                .map(this::fromSpec)
                .collect(Collectors.toList());
    }

}
