[![Travis build Status](https://travis-ci.org/mike10004/selenium-capture.svg?branch=master)](https://travis-ci.org/mike10004/selenium-capture)
[![AppVeyor build status](https://ci.appveyor.com/api/projects/status/fuk4sjvhjl66or7f?svg=true)](https://ci.appveyor.com/project/mike10004/selenium-capture)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.mike10004/selenium-capture.svg)](https://repo1.maven.org/maven2/com/github/mike10004/selenium-capture/)

selenium-capture
=============

This is a library that facilitates capturing HTTP traffic from a Selenium 
web-browsing session. An intercepting proxy is used to capture the traffic.

Example
-------

    // be sure to define system property with geckodriver location if not contained in $PATH
    // System.setProperty("webdriver.gecko.driver", "/path/to/geckodriver");
    FirefoxWebDriverFactory factory = FirefoxWebDriverFactory.builder()
            .configure(firefoxOptions -> {
                firefoxOptions.setAcceptInsecureCerts(true);
            })
            .build();
    Path scratchDir = java.nio.file.Files.createTempDirectory("selenium-capture-example");
    try {
        TrafficCollector collector = TrafficCollector.builder(factory)
                .collectHttps(new AutoCertificateAndKeySource(scratchDir))
                .build();
        HarPlus<String> harPlus = collector.collect(new TrafficGenerator<String>() {
            @Override
            public String generate(WebDriver driver) {
                driver.get("https://www.example.com/");
                return driver.getTitle();
            }
        });
        System.out.println("collected page with title " + harPlus.result);
        File harFile = File.createTempFile("selenium-capture-example", ".har");
        BrowserUpHars.writeHar(harPlus.har, harFile, StandardCharsets.UTF_8);
        System.out.format("%s contains captured traffic%n", harFile);
    } finally {
        FileUtils.forceDelete(scratchDir.toFile());
    }

Capturing HTTPS traffic
-----------------------

To capture HTTPS traffic, the proxy must MITM the TLS traffic and the browser 
must be configured to trust the proxy's certificate or to accept insecure 
certificates. 

The library will generate a self-signed certificate on demand to capture HTTPS
traffic, or you can pre-generate one (take a look at the 
[GenerateNewCertificate][generate-new] class in the test sources).

Generating a new certificate takes up to 1000ms, so if you're frequently 
generating new certificates with the auto-generator, then you might save time 
by pre-generating a certificate and reusing it. For some examples of reusing 
a pre-generated certificate, take a look at the unit tests where HTTPS traffic 
is captured.

Providing cookies to your browser
---------------------------------

The WebDriver APIs for cookie management are a tad simplistic. They have 
annoying snafus like 

* you can't add cookies associated with a site whose page is not open in the 
  browser window, 
* you can't add cookies with all of the attributes the browser is capable of
  storing
* you can't export cookies with all of the original attributes like expiration
  date

This library helps resolve the first two of these snafus for Firefox and 
Chrome webdrivers with custom solutions for each browser.

* For Firefox, the library provides a mechanism to generate the SQLite database
  where the browser stores cookies in the user profile directory 
* For Chrome, the browser is started with an extension that enables cookies
  to be imported programatically

To resolve the issue of cookie export, you can use the traffic capture to 
gain access to all cookies that were sent during a browsing session.

Required Driver Versions
------------------------

Make sure to keep your [Chromedriver][chromedriver-downloads] and 
[Geckodriver][geckodriver-releases] installations up to date. When 
[Selenium][selenium-releases] gets updated, the minimum required versions 
of the driver executables are often raised. 

[geckodriver-releases]: https://github.com/mozilla/geckodriver/releases
[chromedriver-downloads]: https://sites.google.com/a/chromium.org/chromedriver/downloads
[selenium-releases]: https://github.com/SeleniumHQ/selenium/releases