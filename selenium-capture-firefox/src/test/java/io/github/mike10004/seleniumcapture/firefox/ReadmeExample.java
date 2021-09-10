package io.github.mike10004.seleniumcapture.firefox;

import io.github.mike10004.seleniumcapture.AutoCertificateAndKeySource;
import io.github.mike10004.seleniumcapture.HarPlus;
import io.github.mike10004.seleniumcapture.TrafficCollector;
import io.github.mike10004.seleniumcapture.TrafficGenerator;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.mike10004.seleniumcapture.BrowserUpHars;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class ReadmeExample {

    public static void main(String[] args) throws IOException {
        WebDriverManager.firefoxdriver().setup();

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
    }

}
