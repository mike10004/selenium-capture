package com.github.mike10004.seleniumhelp;

interface JavascriptValidator {

    enum ValidationResult {
        GOOD, BAD
    }

    ValidationResult GOOD = ValidationResult.GOOD;
    ValidationResult BAD = ValidationResult.BAD;

    ValidationResult evaluate(String sourceCode);

    static JavascriptValidator getInstance() {
        if (ReflectiveJdk8JavascriptValidator.isConstructible()) {
            return new ReflectiveJdk8JavascriptValidator();
        } else {
            return new RhinoJavascriptValidator();
        }
    }
}

