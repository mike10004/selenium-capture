Changelog
=========

0.37
----

* core API change: provide closeable session from webdriver factory instead of webdriver directly

0.36
----

* make DeserializableCookie string representation more compact

0.35
----

* allow factory for cookie comparators in CookieCollection

0.34
----

* allow other orderings of cookies from HarAnalysis/CookieCollection

0.33
----

* accommodate change in openssl syntax (use -nodes instead of empty password)
* separate pem file creation code from cert/key source
* change API for raw cookie parsing

0.30
----

* allow TrafficCollector subclasses to call constructor that accepts Builder

0.29
----

* use chrome-cookie-implant 1.5.11

0.28
----

* support firefox on windows
* support firefox 58.0+
* use native-helper 8.0.5

0.27
----

* use chrome-cookie-implant 1.5.9
