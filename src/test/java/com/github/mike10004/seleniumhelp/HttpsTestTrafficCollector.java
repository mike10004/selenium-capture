package com.github.mike10004.seleniumhelp;

class HttpsTestTrafficCollector extends TrafficCollector {

    public HttpsTestTrafficCollector(WebDriverFactory webDriverFactory) {
        super(webDriverFactory, TestCertificateAndKeySource.create(),
                AnonymizingFiltersSource.getInstance(), absentUpstreamProxyProvider());
    }
}
