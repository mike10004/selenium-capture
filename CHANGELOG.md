Changelog
=========

0.58
----

* breaking change: packages and classes renamed

0.57
----

* disable support for Chrome
* divide project into multiple modules; projects that declared
  selenium-capture dependency should instead declare selenium-capture-firefox
* support firefox 91 ESR

0.56
----

* upgrade dependencies

0.55
----

* migrate from (no longer maintained) **browsermob-proxy** to (currently 
  maintained) **browserup-proxy**
* use standard methods of installing Firefox extensions

0.54
----

* remove builder methods that support direct instantiation of webdriver 
  capabilities objects; use `configure(Consumer)` instead

0.53
----

* accept webdriver capabilities object modifiers in factory builders
* by default, do not accept insecure certs, even from the intercepting proxy;
  use capabilities object modifier like `options -> options.setAcceptInsecureCerts(true)`
  to allow the browser to accept the certificate from the intercepting proxy

0.52
----

* change project name from selenium-help to selenium-capture
* clarify distinction between upstream proxy and webdriving proxy
* upgrade netty version

0.51
----

* upgrade dependencies

0.50
----

* improve proxy specification API

0.49
----

* remove junit from main classpath (whoops)
* in unit tests, make usage of system properties and environment variables 
  more consistent

0.48
----

* upgrade some dependencies (e.g. Selenium 3.141.59)
* support Firefox headless mode
* avoid bypassing proxy for loopback addresses in Firefox and Chrome
* RejectingFiltersSource removed from main classpath; copy from test sources if you need it
* remove tests that examined insecure http (non-https) interactions; this means the 
  library is not confirmed to be as stable as before, but it's not easy these days
  to force Firefox or Chrome to interact without https

0.47
----

* upgrade some dependencies

0.46
----

* upgrade some dependencies
* add more debug output to unit tests
* migrate to non-deprecated Netty methods 

0.45
----

* specify upstream proxy with URI in TrafficCollector.Builder URI

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
