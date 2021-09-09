package com.github.mike10004.seleniumhelp;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.config.DriverManagerType;

public class DriverManagerSetupCache {

    private static final LoadingCache<DriverManagerType, ?> driverManagerSetupCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<DriverManagerType, Object>() {
                @Override
                public Object load(@SuppressWarnings("NullableProblems") DriverManagerType key) {
                    WebDriverManager.getInstance(key).setup();
                    return new Object();
                }
            });

    private DriverManagerSetupCache() {
    }

    public static void doSetup(DriverManagerType driverManagerType) {
        driverManagerSetupCache.getUnchecked(driverManagerType);
    }
}
