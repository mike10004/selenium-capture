package com.github.mike10004.seleniumhelp;

import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Enumeration of constants representing ways in which a traffic collector may react to an exception
 * thrown during traffic collection.
 */
public interface ExceptionReactor {


    void reactTo(Exception exception) throws IOException, RuntimeException;

    /**
     * Suppress the exception and return a null result.
     */
    ExceptionReactor SUPPRESS = exception -> {};

    /**
     * Log the exception and return a null result.
     */
    ExceptionReactor LOG_AND_SUPPRESS = exception -> LoggerFactory.getLogger(TrafficCollectorImpl.class).error("exception thrown during traffic collection", exception);

    /**
     * Allow the exception to propagate up the stack.
     */
    ExceptionReactor PROPAGATE = exception -> {
        checkArgument(exception instanceof RuntimeException || exception instanceof IOException);
        if (exception instanceof IOException) {
            throw (IOException) exception;
        }
        throw (RuntimeException) exception;
    };
}
