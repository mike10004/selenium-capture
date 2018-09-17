Changelog
=========

0.44
----

* use comma to delimit proxy host bypasses for both Firefox and Chrome

0.43
----

* rename `WebDriverConfig` to `WebdrivingConfig`
* use URIs for proxy specification because URIs are great
* suppress stack trace on decoder error when capturing responses
* raise Apache `httpclient` dependency version to 4.5.6

0.42
----

* use modified litteproxy dependency to support SOCKS upstream proxies (thanks to @jbaldassari) 

0.41
----

* provide access in `TrafficMonitor` to webdriving session created for traffic collection
* move `TrafficCollectorImpl.Builder` to `TrafficCollector` interface

0.40
----

* add missing json adapter for `lastAccessed` field
* be correct but lenient in determining best expiry

0.39
----

* use `Instant` instead of `Date` in cookie implementation
* on import of firefox cookies, explicitly set localhost as base domain (where appropriate)

0.38
----

* support brotli decoding in HAR capture by default
* support alternative reactions to exceptions from `TrafficGenerator`

0.37
----

* core API change: provide closeable session from webdriver factory instead of webdriver directly

0.36
----

* make `DeserializableCookie` string representation more compact

0.35
----

* allow factory for cookie comparators in `CookieCollection`

0.34
----

* allow other orderings of cookies from `HarAnalysis`/`CookieCollection`

0.33
----

* accommodate change in openssl syntax (use -nodes instead of empty password)
* separate pem file creation code from cert/key source
* change API for raw cookie parsing

0.30
----

* allow `TrafficCollector` subclasses to call constructor that accepts `Builder`

0.29
----

* use `chrome-cookie-implant` 1.5.11

0.28
----

* support firefox on windows
* support firefox 58.0+
* use native-helper 8.0.5

0.27
----

* use chrome-cookie-implant 1.5.9
