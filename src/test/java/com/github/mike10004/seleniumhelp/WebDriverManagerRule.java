package com.github.mike10004.seleniumhelp;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.junit.rules.ExternalResource;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static java.util.Objects.requireNonNull;

/**
 * Rule that downloads geckodriver and sets the system properties that specify
 * the geckodriver path. Use this as a {@link org.junit.ClassRule}.
 */
public class WebDriverManagerRule extends ExternalResource {

    private static final Cache<String, String> lockManager = CacheBuilder.newBuilder().build();

    private final Runnable runner;
    private final String identifier;

    public WebDriverManagerRule(Runnable runner, String identifier) {
        this.runner = requireNonNull(runner);
        this.identifier = requireNonNull(identifier);
    }

    @Override
    protected void before() throws ExecutionException {
        lockManager.get(identifier, new Callable<String>() {
            @Override
            public String call() {
                runner.run();
                return identifier;
            }
        });
    }

    private static final WebDriverManagerRule geckodriverSetupRule = new WebDriverManagerRule(UnitTests::setupRecommendedGeckoDriver, "gecko");

    private static final WebDriverManagerRule chromedriverSetupRule = new WebDriverManagerRule(UnitTests::setupRecommendedChromeDriver, "chrome");

    public static WebDriverManagerRule chromedriver() {
        return chromedriverSetupRule;
    }

    public static WebDriverManagerRule geckodriver() {
        return geckodriverSetupRule;
    }
}
