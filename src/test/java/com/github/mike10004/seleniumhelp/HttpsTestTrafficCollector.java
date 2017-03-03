package com.github.mike10004.seleniumhelp;

class HttpsTestTrafficCollector {

    public static TrafficCollector build(WebDriverFactory webDriverFactory) {
        return TrafficCollector.builder(webDriverFactory)
                .collectHttps(TestCertificateAndKeySource.create())
                .build();
    }

}
