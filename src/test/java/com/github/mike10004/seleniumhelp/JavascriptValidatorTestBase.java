package com.github.mike10004.seleniumhelp;

import org.junit.Test;

import static org.junit.Assert.*;

public abstract class JavascriptValidatorTestBase {

    protected abstract JavascriptValidator createValidator();

    @Test
    public void evaluate_good() {

        String sourceCode = "function add(a, b) {\n" +
                "  return a + b;\n" +
                "}\n" +
                "\n" +
                "console.info(add(2, 2));\n";

        JavascriptValidator validator = createValidator();
        JavascriptValidator.ValidationResult result = validator.evaluate(sourceCode);
        assertEquals("result", JavascriptValidator.GOOD, result);
    }

    @Test
    public void evaluate_bad_smokeTest() {
        String sourceCode = "@";
        JavascriptValidator validator = createValidator();
        JavascriptValidator.ValidationResult result = validator.evaluate(sourceCode);
        assertEquals("result", JavascriptValidator.BAD, result);

    }


}