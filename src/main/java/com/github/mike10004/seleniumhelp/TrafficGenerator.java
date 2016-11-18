package com.github.mike10004.seleniumhelp;

import org.openqa.selenium.WebDriver;

import java.io.IOException;

public interface TrafficGenerator {
    void generate(WebDriver driver) throws IOException;
}
