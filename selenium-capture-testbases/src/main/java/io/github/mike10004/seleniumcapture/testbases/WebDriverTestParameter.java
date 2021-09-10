package io.github.mike10004.seleniumcapture.testbases;

import io.github.mike10004.seleniumcapture.WebDriverFactory;
import com.github.mike10004.xvfbtesting.XvfbRule;
import io.github.bonigarcia.wdm.config.DriverManagerType;

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

