package com.github.mike10004.seleniumhelp;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;
import org.slf4j.LoggerFactory;

public class RhinoJavascriptValidator implements JavascriptValidator {
    @Override
    public ValidationResult evaluate(String sourceCode) {
        try {
             parseAndIgnore(sourceCode);
             return GOOD;
        } catch (RuntimeException e) {
            LoggerFactory.getLogger(getClass()).debug("javascript evaluation failed");
            return BAD;
        }
    }

    public void parseAndIgnore(String sourceCode) {
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        compilerEnv.setStrictMode(false);
        Parser p = new Parser(compilerEnv);
        p.parse(sourceCode, "input.js", 1);
    }
}
