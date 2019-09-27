package com.github.mike10004.seleniumhelp;

public class ReflectiveJdk8JavascriptValidatorTest extends JavascriptValidatorTestBase {
    @Override
    protected JavascriptValidator createValidator() {
        return new ReflectiveJdk8JavascriptValidator();
    }
}
