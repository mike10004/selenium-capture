package com.github.mike10004.seleniumhelp;

import static org.junit.Assert.*;

public class RhinoJavascriptValidatorTest extends JavascriptValidatorTestBase {

    @Override
    protected JavascriptValidator createValidator() {
        return new RhinoJavascriptValidator();
    }
}