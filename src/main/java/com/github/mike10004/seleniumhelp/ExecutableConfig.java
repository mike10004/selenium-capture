package com.github.mike10004.seleniumhelp;

import com.github.mike10004.nativehelper.Whicher;
import com.github.mike10004.nativehelper.subprocess.Subprocess;

import javax.annotation.Nullable;
import java.io.File;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Interface that provides a method to create a subprocess builder for an executable.
 */
public interface ExecutableConfig {
    /**
     * Creates a new subprocess builder.
     * @return the builder
     */
    Subprocess.Builder subprocessBuilder();

    /**
     * Checks whether the executable is available.
     * Checks whether the executable is available.
     * @return true if the executable is executable
     */
    boolean isExecutableAvailable();

    class BasicExecutableConfig implements ExecutableConfig {
        @Nullable
        private final File executablePathname;
        private final String executableFilename;

        BasicExecutableConfig(@Nullable File executablePathname, String executableFilename) {
            this.executablePathname = executablePathname;
            this.executableFilename = executableFilename;
            checkArgument(executablePathname != null || executableFilename != null);
        }

        protected Whicher getWhicher() {
            return Whicher.gnu();
        }

        @Override
        public boolean isExecutableAvailable() {
            if (executablePathname != null) {
                if (executablePathname.isFile() && executablePathname.canExecute()) {
                    return true;
                }
            }
            return getWhicher().which(executableFilename).isPresent();
        }

        @Override
        public Subprocess.Builder subprocessBuilder() {
            if (executablePathname == null) {
                return Subprocess.running(executableFilename);
            } else {
                return Subprocess.running(executablePathname);
            }
        }

    }

    static ExecutableConfig byNameOnly(String executableName) {
        return new BasicExecutableConfig(null, executableName);
    }

    static ExecutableConfig byPathOnly(File executableFile) {
        return new BasicExecutableConfig(executableFile, null);
    }
}
