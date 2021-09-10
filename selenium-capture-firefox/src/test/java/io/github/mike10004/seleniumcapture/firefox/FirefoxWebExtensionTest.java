package io.github.mike10004.seleniumcapture.firefox;

import com.browserup.harreader.model.Har;
import com.github.mike10004.seleniumhelp.HarPlus;
import com.github.mike10004.seleniumhelp.WebDriverFactory;
import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.collect.ImmutableMap;
import io.github.mike10004.nanochamp.server.NanoControl;
import io.github.mike10004.nanochamp.server.NanoResponse;
import io.github.mike10004.nanochamp.server.NanoServer;
import io.github.mike10004.seleniumcapture.testbases.CollectionTestBase;
import io.github.mike10004.seleniumcapture.testbases.UpstreamProxyRule;
import io.github.mike10004.seleniumcapture.testbases.WebDriverManagerRule;
import io.github.mike10004.seleniumcapture.testing.UnitTests;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FirefoxWebExtensionTest {

    @ClassRule
    public static final XvfbRule xvfb = UnitTests.xvfbRuleBuilder().build();

    @ClassRule
    public static WebDriverManagerRule webDriverManagerRule = WebDriverManagerRule.geckodriver();

    @Test
    public void useWebExtensionsExtensionZip_unsigned() throws Exception {
        Supplier<FirefoxBinary> firefoxBinarySupplier = FirefoxUnitTests.createFirefoxBinarySupplier();
        FirefoxUnitTests.requireEsrOrUnbrandedFirefoxBinary(firefoxBinarySupplier);
        File zipFile = prepareExtensionZipFile();
        List<String> targetTexts = new ArrayList<>();
        useWebExtensionsExtensionZip(zipFile, FirefoxWebDriverFactory.XpinstallSetting.SIGNATURE_REQUIRED_FALSE,
                driver -> {
                    driver.get("https://www.example.com/");
                    WebElement element = new WebDriverWait(driver, Duration.ofSeconds(5))
                            .until(ExpectedConditions.presenceOfElementLocated(By.id("firefox-webext-content-injection")));
                    targetTexts.add(element.getText());
                }, firefoxBinarySupplier);
        String elementText = targetTexts.get(0);
        assertEquals("element text", "Hello, world", elementText);
    }

    @Test
    public void useWebExtensionsExtensionZip_signed() throws Exception {
        URL xpiResource = requireNonNull(getClass().getResource("/dark_background_and_light_text-0.7.6-an+fx.xpi"));
        File zipFile = new File(xpiResource.toURI());
        NanoServer server = NanoServer.builder()
                .getPath("/", session -> NanoResponse.status(200).htmlUtf8(smallPageHtml()))
                .build();
        useWebExtensionsExtensionZip(zipFile, FirefoxWebDriverFactory.XpinstallSetting.NOT_MODIFIED,
                driver -> {
                    try (NanoControl ctrl = server.startServer()) {
                        checkDarkReaderContent(driver, ctrl.baseUri());
                    }
                });
    }

    private static String smallPageHtml() {
        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>\n" +
                "  <title>Information</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div id=\"main\">Hello, world</div>\n" +
                "</body>\n" +
                "</html>\n";
        return html;
    }

    private void checkDarkReaderContent(WebDriver webdriver, URI url) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) webdriver;
        webdriver.get(url.toString());
        try {
            Thread.sleep(250); // allow time for the extension script to execute
        } catch (InterruptedException ignore) {
        }
        String backgroundColor = (String) jsExecutor.executeScript(scriptThatReturnsComputedStyleOfElementById("main", "background-color"));
        MatcherAssert.assertThat("background-color", backgroundColor, Matchers.matchesPattern("^\\s*rgba?\\(0(,\\s*0){2,3}\\)\\s*$"));
        String color = (String) jsExecutor.executeScript(scriptThatReturnsComputedStyleOfElementById("main", "color"));
        MatcherAssert.assertThat("color", color, Matchers.matchesPattern("^\\s*rgba?\\(255(,\\s*255){2,3}\\)\\s*$"));
    }


    @SuppressWarnings("SameParameterValue")
    private static String scriptThatReturnsComputedStyleOfElementById(String elementId, String cssProperty) {
        elementId = StringEscapeUtils.escapeEcmaScript(elementId);
        cssProperty = StringEscapeUtils.escapeEcmaScript(cssProperty);
        String js = "let elt = document.getElementById('" + elementId + "');\n" +
                "let style = document.defaultView.getComputedStyle(elt);\n" +
                "return style['" + cssProperty + "'];";
        return js;
    }

    private interface Driveable {
        void accept(WebDriver webdriver) throws Exception;
    }

    @SuppressWarnings("UnusedReturnValue")
    private Har useWebExtensionsExtensionZip(File zipFile,
                                             @SuppressWarnings("SameParameterValue") FirefoxWebDriverFactory.XpinstallSetting xpinstallSetting,
                                             Driveable driveable) throws Exception {
        return useWebExtensionsExtensionZip(zipFile, xpinstallSetting, driveable, FirefoxUnitTests.createFirefoxBinarySupplier());
    }

    @SuppressWarnings("UnusedReturnValue")
    private Har useWebExtensionsExtensionZip(File zipFile,
                                             FirefoxWebDriverFactory.XpinstallSetting xpinstallSetting,
                                             Driveable driveable,
                                             Supplier<FirefoxBinary> firefoxBinarySupplier) throws Exception {
        String display = xvfb.getController().getDisplay();
        WebDriverFactory webDriverFactory = FirefoxWebDriverFactory.builder()
                .binary(firefoxBinarySupplier)
                .environment(createEnvironmentSupplierForDisplay(display))
                .acceptInsecureCerts()
                .profileAction(profile -> {
                    profile.setPreference("extensions.logging.enabled", true);
                }).profileAction(xpinstallSetting)
                .build();
        AtomicReference<Exception> error = new AtomicReference<>(null);
        // Using a local HTTP server seems to cause some trouble here, so we visit example.com instead
        HarPlus<Void> harPlus = CollectionTestBase.testTrafficCollector(webDriverFactory, driver -> {
            ((FirefoxDriver) driver).installExtension(zipFile.toPath());
            try {
                driveable.accept(driver);
            } catch (Exception e) {
                error.set(e);
            }
            return null;
        }, "https", upstreamProxyRule::getProxySpecificationUri);
        assertNull("exception while webdriving", error.get());
        return harPlus.har;
    }

    @ClassRule
    public static final UpstreamProxyRule upstreamProxyRule = new UpstreamProxyRule();

    private static final String EXAMPLE_EXTENSION_MANIFEST_PATH =
            "/firefox-example-extension/manifest.json";

    private File prepareExtensionZipFile() throws IOException, URISyntaxException {
        URL resource = getClass().getResource(EXAMPLE_EXTENSION_MANIFEST_PATH);
        requireNonNull(resource, EXAMPLE_EXTENSION_MANIFEST_PATH);
        File directory = new File(resource.toURI()).getParentFile();
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

    private static class EmptyEnvironmentSupplier implements Supplier<Map<String, String>> {

        public EmptyEnvironmentSupplier() {}

        @Override
        public Map<String, String> get() {
            return ImmutableMap.of();
        }

        @Override
        public String toString() {
            return "EmptyEnvironment{}";
        }
    }

    private static Supplier<Map<String, String>> createEnvironmentSupplierForDisplay(@Nullable String display) {
        if (display == null) {
            return new EmptyEnvironmentSupplier();
        } else {
            return new Supplier<Map<String, String>>() {
                @Override
                public Map<String, String> get() {
                    return ImmutableMap.of("DISPLAY", display);
                }

                @Override
                public String toString() {
                    return String.format("Environment{DISPLAY=%s}", StringUtils.abbreviate(display, 16));
                }
            };
        }
    }

}
