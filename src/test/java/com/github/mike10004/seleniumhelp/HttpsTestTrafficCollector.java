package com.github.mike10004.seleniumhelp;

class HttpsTestTrafficCollector extends TrafficCollector {

    public HttpsTestTrafficCollector(WebDriverFactory webDriverFactory) {
        super(webDriverFactory, new TestCertificateAndKeySource(),
                AnonymizingFiltersSource.getInstance(), absentUpstreamProxyProvider());
    }
}
