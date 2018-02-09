package com.github.mike10004.seleniumhelp;

import com.github.mike10004.seleniumhelp.FirefoxWebDriverFactory.CookieInstallingProfileAction;
import com.github.mike10004.seleniumhelp.FirefoxWebDriverFactory.FirefoxProfileFolderAction;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringExclude;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("Duplicates")
public class FirefoxProfileExperiment {

    @ClassRule
    public static GeckodriverSetupRule geckodriverSetupRule = new GeckodriverSetupRule();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @org.junit.Ignore
    @Test
    public void doSomething() throws Exception {
        Path scratchDir = FileUtils.getTempDirectory().toPath();
        WebDriverConfig config = WebDriverConfig.builder()
                .certificateAndKeySource(new AutoCertificateAndKeySource(scratchDir)).build();
        WebDriver driver = FirefoxWebDriverFactory
                .builder()
                .build().createWebDriver(config);
        try {
            driver.get("http://example.com/");
        } finally {
            driver.quit();
        }
    }

    @Test
    public void provideProfileDirWithCookiesDbFile() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        String cookieJson = "{\n" +
                "    \"name\": \"echo_cookie\",\n" +
                "    \"value\": \"3c73dfc224354c9389d5de350e93d9d3\",\n" +
                "    \"attribs\": {\n" +
                "      \"domain\": \"httprequestecho.appspot.com\",\n" +
                "      \"version\": \"1\",\n" +
                "      \"path\": \"/\",\n" +
                "      \"max-age\": \"2592000\"\n" +
                "    },\n" +
                "    \"cookieDomain\": \"httprequestecho.appspot.com\",\n" +
                "    \"cookieExpiryDate\": \"Mar 4, 2018 2:31:41 PM\",\n" +
                "    \"cookiePath\": \"/\",\n" +
                "    \"isSecure\": false,\n" +
                "    \"cookieVersion\": 0,\n" +
                "    \"creationDate\": \"Feb 2, 2018 2:31:41 PM\",\n" +
                "    \"httpOnly\": false\n" +
                "  }";
        DeserializableCookie cookie = new Gson().fromJson(cookieJson, DeserializableCookie.class);
        File profileDir = temporaryFolder.newFolder();
        File sqliteDbFile = new File(profileDir, "cookies.sqlite");
        Resources.asByteSource(getClass().getResource("/empty-firefox-cookies-db.sqlite")).copyTo(Files.asByteSink(sqliteDbFile));
        FirefoxCookieDb.getImporter().importCookies(Collections.singleton(cookie), sqliteDbFile);
        Files.asByteSource(sqliteDbFile).copyTo(Files.asByteSink(new File("/tmp/provideProfileDirWithCookiesDbFile.sqlite")));
        System.out.format("starting with %s%n", profileDir);
        FirefoxProfile profile = new FirefoxProfile(profileDir) {
            @Override
            public File layoutOnDisk() {
                counter.incrementAndGet();
                File f = super.layoutOnDisk();
                return f;
            }
        };
        FirefoxOptions options = new FirefoxOptions();
        options.setProfile(profile);
        FirefoxDriver webdriver = new FirefoxDriver(options);
        HttpRequestEchoContent content;
        try {
            webdriver.get("https://httprequestecho.appspot.com/get");
            String pageText = webdriver.getPageSource();
            pageText = removeHtmlTags(pageText);
            System.out.println(pageText);
            content = new Gson().fromJson(pageText, HttpRequestEchoContent.class);
        } finally {
            webdriver.quit();
        }
        System.out.println();
        assertNotNull("headers", content.headers);
        List<String> cookieValues = content.headers.get(HttpHeaders.COOKIE);
        assertNotNull("cookieValues", cookieValues);
        assertFalse("nonempty cookies list", cookieValues.isEmpty());
//        System.out.format("laid outs: %s%n", laidOut);
    }

    @Test
    public void webdriverfactory_modifyCookiesDbInProfileDir() throws Exception {
        String cookieJson = "{\n" +
                "    \"name\": \"echo_cookie\",\n" +
                "    \"value\": \"3c73dfc224354c9389d5de350e93d9d3\",\n" +
                "    \"attribs\": {\n" +
                "      \"domain\": \"httprequestecho.appspot.com\",\n" +
                "      \"version\": \"1\",\n" +
                "      \"path\": \"/\",\n" +
                "      \"max-age\": \"2592000\"\n" +
                "    },\n" +
                "    \"cookieDomain\": \"httprequestecho.appspot.com\",\n" +
                "    \"cookieExpiryDate\": \"Mar 4, 2018 2:31:41 PM\",\n" +
                "    \"cookiePath\": \"/\",\n" +
                "    \"isSecure\": false,\n" +
                "    \"cookieVersion\": 0,\n" +
                "    \"creationDate\": \"Feb 2, 2018 2:31:41 PM\",\n" +
                "    \"httpOnly\": false\n" +
                "  }";
        DeserializableCookie cookie = new Gson().fromJson(cookieJson, DeserializableCookie.class);
        WebDriverConfig config = WebDriverConfig.builder()
                .certificateAndKeySource(new AutoCertificateAndKeySource(temporaryFolder.getRoot().toPath())).build();
        AtomicInteger pfaPerformCount = new AtomicInteger(0);
        FirefoxWebDriverFactory.Builder builder = FirefoxWebDriverFactory.builder()
                .addCookies(Collections.singleton(cookie))
                .profileFolderAction(new FirefoxProfileFolderAction() {
                    @Override
                    public void perform(File profileDir) {
                        File sqliteDbFile = new File(profileDir, "cookies.sqlite");
                        checkState(sqliteDbFile.isFile(), "not a file: %s", sqliteDbFile);
                        File dest = new File("/tmp/webdriverfactory_modifyCookiesDbInProfileDir-" + pfaPerformCount.incrementAndGet() + ".sqlite");
                        System.out.println("created " + dest);
                        try {
                            FileUtils.copyFile(sqliteDbFile, dest);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
        AtomicReference<String> zipBase64Ref = new AtomicReference<>();
        FirefoxWebDriverFactory factory = new FirefoxWebDriverFactory(builder) {
            @Override
            protected SupplementingFirefoxProfile createFirefoxProfile(List<FirefoxProfileFolderAction> actions) {
                return new SupplementingFirefoxProfile(actions) {
                    @Override
                    public String toJson() throws IOException {
                        String json = super.toJson();
                        zipBase64Ref.set(json);
                        return json;
                    }
                };
            }
        };
        WebDriver webdriver = factory.createWebDriver(config);
        HttpRequestEchoContent content;
        try {
            webdriver.get("https://httprequestecho.appspot.com/get");
            String pageText = webdriver.getPageSource();
            pageText = removeHtmlTags(pageText);
            System.out.println(pageText);
            content = new Gson().fromJson(pageText, HttpRequestEchoContent.class);
        } finally {
            webdriver.quit();
        }
        System.out.println();
        String zipBase64 = zipBase64Ref.get();
        byte[] zipBytes = Base64.getDecoder().decode(zipBase64);
        File zipFile = temporaryFolder.newFile();
        Files.write(zipBytes, zipFile);
        Unzippage unzippage = Unzippage.unzip(zipFile);
        unzippage.fileEntries().forEach(System.out::println);
        String cookiesEntry = StreamSupport.stream(unzippage.fileEntries().spliterator(), false).filter(e -> e.contains("cookies.sqlite")).findFirst().orElse(null);
        assertNotNull(cookiesEntry);
        byte[] cookiesDbBytes = unzippage.getFileBytes(cookiesEntry).read();
//        File
        assertNotNull("headers", content.headers);
        List<String> cookieValues = content.headers.get(HttpHeaders.COOKIE);
        assertNotNull("cookieValues", cookieValues);
        assertFalse("nonempty cookies list", cookieValues.isEmpty());
//        System.out.format("laid outs: %s%n", laidOut);
    }

    private static String removeHtmlTags(String pageHtml) {
        Elements els = Jsoup.parse(pageHtml).select("plaintext");
        Element element = els.first();
        String text = element.text();
        return StringUtils.removeEnd(text, "</plaintext></div></body></html>");
    }
}
