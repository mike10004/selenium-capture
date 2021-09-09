package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbtesting.XvfbRule;
import io.github.bonigarcia.wdm.config.DriverManagerType;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface WebDriverTestParameter {

    WebDriverFactory createWebDriverFactory(XvfbRule xvfb);

    /**
     * Checks whether the test should check that the page text is what was expected.
     */
    boolean isBrotliSupported(String url);

    DriverManagerType getDriverManagerType();

//    static List<WebDriverTestParameter> all() {
//        return createList(x -> true, false);
//    }
//
//    static List<WebDriverTestParameter> allAcceptingInsecureCerts() {
//        return createList(x -> true, true);
//    }

//    static List<WebDriverTestParameter> createList(Predicate<? super DriverManagerType> typePredicate, boolean acceptInsecureCerts) {
//        return Stream.of(new FirefoxTestParameter(acceptInsecureCerts), new ChromeTestParameter(acceptInsecureCerts))
//                .filter(p -> typePredicate.test(p.getDriverManagerType()))
//                .collect(Collectors.toList());
//    }

    default void doDriverManagerSetup() {
        DriverManagerSetupCache.doSetup(getDriverManagerType());
    }

}

