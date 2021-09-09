package com.github.mike10004.seleniumhelp;

class HttpsTestTrafficCollector {

    public static TrafficCollector build(WebDriverFactory webDriverFactory) {
        return builder(webDriverFactory).build();
    }

    public static TrafficCollector.Builder builder(WebDriverFactory webDriverFactory) {
//        return TrafficCollector.builder(webDriverFactory)
//                .collectHttps(TestCertificateAndKeySource.create());
        throw new UnsupportedOperationException("just invoke return TrafficCollector.builder(webDriverFactory).collectHttps(TestCertificateAndKeySource.create()) directly");
    }

}
