package com.github.mike10004.seleniumhelp;

import java.io.File;

/**
 * Interface that provides a method to create a subprocess builder for an executable.
 */
public interface ExecutableConfig {
    /**
     * Gets the name of the executable. This may be either the pathname
     * or a basename to be resolved using the system PATH environment variable.
     */
    String getExecutableName();

    /**
     * Checks whether the executable is available.
     * @return true if the executable is executable
     */
    boolean isExecutableAvailable();

    static ExecutableConfig byNameOnly(String executableName) {
        return new StringExecutableConfig(executableName);
    }

    static ExecutableConfig byPathOnly(File executableFile) {
        return new FileExecutableConfig(executableFile);
    }
}
