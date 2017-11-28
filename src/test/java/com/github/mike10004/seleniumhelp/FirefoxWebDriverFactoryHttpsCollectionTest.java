package com.github.mike10004.seleniumhelp;

import io.github.mike10004.extensibleffdriver.AddonInstallRequest;
import io.github.mike10004.extensibleffdriver.AddonPersistence;
import io.github.mike10004.extensibleffdriver.ExtensibleFirefoxDriver;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;

public class FirefoxWebDriverFactoryHttpsCollectionTest extends CollectionTestBase {

    public FirefoxWebDriverFactoryHttpsCollectionTest() {
        super("https");
    }

    @BeforeClass
    public static void setUpDriver() {
        String driverPath = System.getProperty("webdriver.gecko.driver");
        if (driverPath == null) {
            UnitTests.setupRecommendedGeckoDriver();
        }
    }

    @Test
    public void https() throws Exception {
        String display = xvfb.getController().getDisplay();
        WebDriverFactory webDriverFactory = FirefoxWebDriverFactory.builder()
            .environment(FirefoxWebDriverFactory.createEnvironmentSupplierForDisplay(display))
            .build();
        testTrafficCollector(webDriverFactory);
    }

    @Test
    public void useWebExtensionsExtensionZip() throws Exception {
        File zipFile = prepareExtensionZipFile();
        String display = xvfb.getController().getDisplay();
        WebDriverFactory webDriverFactory = FirefoxWebDriverFactory.builder()
                .environment(FirefoxWebDriverFactory.createEnvironmentSupplierForDisplay(display))
                .constructor((service, options) -> {
                    ExtensibleFirefoxDriver driver = new ExtensibleFirefoxDriver(service, options);
                    driver.installAddon(AddonInstallRequest.fromFile(zipFile, AddonPersistence.TEMPORARY));
                    return driver;
                }).build();
        // Using a local HTTP server seems to cause some trouble here, so we visit example.com instead
        HarPlus<String> injectedContentCollection = testTrafficCollector(webDriverFactory, driver -> {
            driver.get("https://www.example.com/");
            WebElement element = new WebDriverWait(driver, 5)
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("firefox-webext-content-injection")));
            return element.getText();
        });
        String elementText = injectedContentCollection.result.trim();
        assertEquals("element text", "Hello, world", elementText);
    }

    private File prepareExtensionZipFile() throws IOException, URISyntaxException {
        File directory = new File(getClass().getResource("/firefox-example-extension/manifest.json").toURI()).getParentFile();
        Collection<File> extensionFiles = FileUtils.listFiles(directory, null, true);
        File zipFile = File.createTempFile("firefox-example-extension", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile, false))) {
            for (File extensionFile : extensionFiles) {
                Path source = extensionFile.toPath();
                ZipEntry entry = new ZipEntry(directory.toPath().relativize(source).toString());
                zos.putNextEntry(entry);
                zos.write(java.nio.file.Files.readAllBytes(source));
                zos.closeEntry();
            }
        }
        return zipFile;
    }


}