package com.github.mike10004.seleniumhelp;

import java.io.File;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of an executable config that allows you to use a
 * pathname or just the name of an executable. If just the executable
 * is used, its absolute path is resolved against directories in
 * the system PATH environment variable.
 */
class FileExecutableConfig implements ExecutableConfig {

    private final File executablePathname;

    public FileExecutableConfig(File executablePathname) {
        this.executablePathname = requireNonNull(executablePathname);
    }

    @Override
    public boolean isExecutableAvailable() {
        return executablePathname.isFile() && executablePathname.canExecute();
    }

    @Override
    public String getExecutableName() {
        return executablePathname.getAbsolutePath();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FileExecutableConfig.class.getSimpleName() + "[", "]")
                .add("executablePathname=" + executablePathname)
                .toString();
    }
}
