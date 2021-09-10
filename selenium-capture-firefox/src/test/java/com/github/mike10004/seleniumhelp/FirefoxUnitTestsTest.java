package com.github.mike10004.seleniumhelp;

import org.junit.Test;
import org.openqa.selenium.firefox.FirefoxBinary;

import static org.junit.Assert.assertTrue;

public class FirefoxUnitTestsTest {

    @Test
    public void createFirefoxBinarySupplier() {
        FirefoxBinary b = FirefoxUnitTests.createFirefoxBinarySupplier().get();
        System.out.format("%s is firefox binary%n", b);
        assertTrue("exists", b.getFile().isFile());
        assertTrue("canExecute", b.getFile().canExecute());
    }

}
