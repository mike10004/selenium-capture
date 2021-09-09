package com.github.mike10004.seleniumhelp;

import com.github.mike10004.nativehelper.Whicher;

import java.util.StringJoiner;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

class StringExecutableConfig implements ExecutableConfig {

    private final String executableName;
    private final Whicher whicher;

    public StringExecutableConfig(String executableName) {
        this(executableName, Whicher.gnu());
    }

    public StringExecutableConfig(String executableName, Whicher whicher) {
        this.executableName = requireNonNull(executableName);
        checkArgument(!executableName.isEmpty(), "name must be nonempty");
        this.whicher = requireNonNull(whicher);
    }

    @Override
    public String getExecutableName() {
        return executableName;
    }

    @Override
    public boolean isExecutableAvailable() {
        return whicher.which(executableName).isPresent();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", StringExecutableConfig.class.getSimpleName() + "[", "]")
                .add("executableName='" + executableName + "'")
                .add("whicher=" + whicher)
                .toString();
    }
}
