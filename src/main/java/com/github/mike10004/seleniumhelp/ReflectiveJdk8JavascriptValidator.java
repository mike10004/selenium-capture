package com.github.mike10004.seleniumhelp;

import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

@SuppressWarnings("SameParameterValue")
class ReflectiveJdk8JavascriptValidator implements JavascriptValidator {

    @Override
    public ValidationResult evaluate(String sourceCode) {
        try {
            return new Reflector().evaluateChecked(sourceCode) ? BAD : GOOD;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("nashorn javascript parser is not available or API is not as expected");
        }
    }

    public static boolean isConstructible() {
        try {
            new Reflector();
            return true;
        } catch (ReflectiveOperationException ignore) {
            return false;
        }
    }

    private static class Reflector {

        private static final String OPTIONS_CLASSNAME = "jdk.nashorn.internal.runtime.options.Options";
        private static final String ERRORMANAGER_CLASSNAME = "jdk.nashorn.internal.runtime.ErrorManager";
        private static final String CONTEXT_CLASSNAME = "jdk.nashorn.internal.runtime.Context";
        private static final String SOURCE_CLASSNAME = "jdk.nashorn.internal.runtime.Source";
        private static final String PARSER_CLASSNAME = "jdk.nashorn.internal.parser.Parser";

        public final Class<?> ErrorManager;
        public final Class<?> Options;
        public final Class<?> Context;
        public final Class<?> Parser;
        public final Class<?> Source;
        public final Class<?> ScriptEnvironment;

        public Reflector() throws ClassNotFoundException {
            ErrorManager = Class.forName(ERRORMANAGER_CLASSNAME);
            Options = Class.forName(OPTIONS_CLASSNAME);
            Context = Class.forName(CONTEXT_CLASSNAME);
            Parser = Class.forName(PARSER_CLASSNAME);
            Source = Class.forName(SOURCE_CLASSNAME);
            ScriptEnvironment = Class.forName("jdk.nashorn.internal.runtime.ScriptEnvironment");
        }

        private Object newOptions(String resource) throws ReflectiveOperationException {
            return Options.getConstructor(String.class).newInstance(resource);
        }

        private void Options_set(Object instance, String property, boolean value) throws ReflectiveOperationException {
            Method m = getMethod(Options, instance, "set", String.class, Boolean.TYPE);
            m.invoke(instance, property, value);
        }

        static Method getMethod(Class<?> clz, Object instance, String methodName, Class<?>...paramTypes) throws ReflectiveOperationException {
            // not sure about getMethod vs getDelegateMethod here
            return clz.getMethod(methodName, paramTypes);
        }

        private Object newContext(Object options, Object errorManager, ClassLoader classLoader)  throws ReflectiveOperationException {
            return Class.forName(CONTEXT_CLASSNAME).getConstructor(Options, ErrorManager, ClassLoader.class).newInstance(options, errorManager, classLoader);
        }

        private Object sourceFor(String name, String sourceCode)  throws ReflectiveOperationException {
            return Source.getMethod("sourceFor", String.class, String.class).invoke(null, name, sourceCode);
        }

        private Object newParser(Object scriptEnvironment, Object source, Object errorManager)  throws ReflectiveOperationException {
            return Parser.getConstructor(ScriptEnvironment, Source, ErrorManager).newInstance(scriptEnvironment, source, errorManager);
        }

        private Object Context_getEnv(Object context)  throws ReflectiveOperationException {
            return getMethod(Context, context, "getEnv").invoke(context);
        }

        private void Parser_parse(Object parser) throws ReflectiveOperationException {
            getMethod(Parser, parser, "parse").invoke(parser);
        }

        private boolean ErrorManager_hasErrors(Object errorManager) throws ReflectiveOperationException {
            Boolean b = (Boolean) getMethod(ErrorManager, errorManager, "hasErrors").invoke(errorManager);
            if (b == null) {
                throw new IllegalStateException("expected non-null return value from ErrorManager.hasErrors");
            }
            return b.booleanValue();
        }

        private Object newErrorManager() throws ReflectiveOperationException {
            return ErrorManager.getConstructor().newInstance();
        }

        public boolean evaluateChecked(String sourceCode) throws ReflectiveOperationException {
            Object options = newOptions("nashorn");
            Options_set(options, "anon.functions", true);
            Options_set(options, "parse.only", true);
            Options_set(options, "scripting", true);
            Object errorManager = newErrorManager();
            Object context = newContext(options, errorManager, Thread.currentThread().getContextClassLoader());
            Object source = sourceFor("test", sourceCode);
            Object parser = newParser(Context_getEnv(context), source, errorManager);
            Parser_parse(parser);
            return ErrorManager_hasErrors(errorManager);
        }
    }



}
