package com.github.mike10004.seleniumhelp;

import org.openqa.selenium.WebDriver;

import java.io.IOException;

/**
 *
 * @param <R> result type
 */
public interface TrafficGenerator<R> {

    /**
     * Generates HTTP traffic. Implement this method with code that makes use of a webdriver
     * to automate network activity that will be captured in the har returned by
     * {@link TrafficCollector#collect(TrafficGenerator)}.
     * @param driver the webdriver
     * @return the result
     * @throws IOException if something goes awry
     */
    R generate(WebDriver driver) throws IOException;

}
