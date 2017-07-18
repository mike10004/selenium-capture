package com.github.mike10004.seleniumhelp;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.client.utils.URIBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;

import static com.google.common.base.Preconditions.checkState;

public class ChromeDriverExperiments {

    @SuppressWarnings("SameParameterValue")
    private static String produceCookieArrayJson(URI url, String domain, String path, String name, String value) {
        JsonObject cookie = produceCookieObject(url, domain, path, name, value);
        JsonArray array = new JsonArray();
        array.add(cookie);
        return new Gson().toJson(array);
    }

    private static JsonObject produceCookieObject(URI url, String domain, String path, String name, String value) {
        JsonObject obj = new JsonObject();
        obj.addProperty("url", url.toString());
        obj.addProperty("domain", domain);
        obj.addProperty("path", path);
        obj.addProperty("name", name);
        obj.addProperty("value", value);
        obj.addProperty("httpOnly", true);
//        obj.addProperty("hostOnly", !domain.startsWith("."));
        long expiry = DateUtils.addDays(Calendar.getInstance().getTime(), 7).getTime() / 1000; // next week, in millis
        obj.addProperty("expirationDate", expiry);
        obj.addProperty("secure", false);
        return obj;
    }

    private static class EditThisCookieDoer extends Doer<String> {

        @Override
        public String doStuff(WebDriver driver) {
            String preCookieGet = null, postCookieGet;
            URI url = URI.create("https://httprequestecho.appspot.com/");
            driver.get("chrome-extension://fngmhnnpilhplaeedifhccceomclgfbg/popup.html?url=" + url + "&incognito=false");
            WebElement pasteButton = new WebDriverWait(driver, 3).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a#pasteButton")));
            pasteButton.click();
            WebElement textArea = new WebDriverWait(driver, 3).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div#pasteCookie textarea")));
            String COOKIE_JSON = produceCookieArrayJson(url, "httprequestecho.appspot.com", "/", "SLIM_COOKIE_NAME", "SLIM_COOKIE_VALUE");
            textArea.sendKeys(COOKIE_JSON);
            WebElement importButton = new WebDriverWait(driver, 3).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#submitDiv #submitButton")));
            importButton.click();
            driver.get("https://httprequestecho.appspot.com/get");
            postCookieGet = driver.getPageSource();
            dumpEchoes(preCookieGet, postCookieGet);
            return postCookieGet;
        }

        @Override
        public void prepare(File tmpDir, ChromeOptions options) throws IOException {
            File extensionCrxFile = File.createTempFile("EditThisCookie", ".crx", tmpDir);
            Resources.asByteSource(ChromeDriverExperiments.class.getResource("/EditThisCookie_v1.4.1.crx")).copyTo(com.google.common.io.Files.asByteSink(extensionCrxFile));
            options.addExtensions(extensionCrxFile);
        }
    }

    static class Doer<T> {
        public void prepare(File tmpDir, ChromeOptions options) throws IOException {
        }

        public T doStuff(WebDriver driver) throws IOException {
            return null;
        }

        public boolean shouldRemainOpen(T stuff, WebDriver driver) {
            return true;
        }

        public boolean isRetainUserDataDir() {
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
//        main(new EditThisCookieDoer());
//        main(new Doer());
//        main(new CustomExtensionDoer());
        main(new CookieImplanter());
    }

    private static class CustomExtensionDoer extends Doer<Void> {

        @Override
        public void prepare(File tmpDir, ChromeOptions options) throws IOException {
            File extensionCrxFile = File.createTempFile("my-custom-extension", ".crx", tmpDir);
            File srcFile = new File("/tmp/first-chrome-extension.crx");
            com.google.common.io.Files.copy(srcFile, extensionCrxFile);
            options.addExtensions(extensionCrxFile);
        }

        @Override
        public Void doStuff(WebDriver driver) throws IOException {
            driver.get("https://www.example.com/");
            return null;
        }
    }

    private static class CookieImplanter extends Doer<Boolean> {

        private static final String SYSPROP_CRX_FILE = "chromeCookieImplant.crx.file";
        private static final String SYSPROP_CRX_SOURCES = "chromeCookieImplant.crx.sourceDirectory";
        public static final String EXTENSION_ID = "neiaahbjfbepoclbammdhcailekhmcdm";

        private void checkUpToDate(File fileWithRequiredLastModifiedTime, File directoryOfOtherFiles) throws IOException {
            Collection<File> otherFiles = FileUtils.listFiles(directoryOfOtherFiles, null, true);
            checkState(!otherFiles.isEmpty(), "empty: " + directoryOfOtherFiles);
            long maxLastModifiedInOtherFiles = otherFiles.stream().map(File::lastModified)
                    .max(Long::compareTo).orElse(0L);
            if (maxLastModifiedInOtherFiles > fileWithRequiredLastModifiedTime.lastModified()) {
                throw new IOException("one or more files in " + directoryOfOtherFiles + " has a last modified date greater than " + fileWithRequiredLastModifiedTime);
            }
        }

        protected ByteSource resolveCrxSource() throws IOException {
            return resolveCrxSourceFromSystemProperty().orElse(Resources.asByteSource(getClass().getResource("/chrome-cookie-implant-1.1.crx")));
        }

        protected Optional<ByteSource> resolveCrxSourceFromSystemProperty() throws IOException {
            String crxPathname = System.getProperty(SYSPROP_CRX_FILE);
            if (crxPathname == null) {
                return Optional.empty();
            }
            File srcFile = new File(SYSPROP_CRX_FILE);
            if (!srcFile.isFile()) {
                throw new FileNotFoundException(srcFile.getAbsolutePath());
            }
            String srcPathname = System.getProperty(SYSPROP_CRX_SOURCES);
            if (srcPathname != null) {
                checkUpToDate(srcFile, new File(srcPathname));
            }
            return Optional.of(com.google.common.io.Files.asByteSource(srcFile));
        }

        @Override
        public void prepare(File tmpDir, ChromeOptions options) throws IOException {
            File extensionCrxFile = File.createTempFile("chrome-cookie-implant", ".crx", tmpDir);
            ByteSource crxSource = resolveCrxSource();
            crxSource.copyTo(com.google.common.io.Files.asByteSink(extensionCrxFile));
            options.addExtensions(extensionCrxFile);
        }


        private final Random random = new Random();

        private String newRandomCookieValue() {
            byte[] bytes = new byte[64];
            random.nextBytes(bytes);
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
            return base64;
        }

        @Override
        public Boolean doStuff(WebDriver driver) throws IOException {
            String[] cookieJsons = {
                new Gson().toJson(produceCookieObject(URI.create("https://httprequestecho.appspot.com/"), "httprequestecho.appspot.com", "/", "IMPLANTED_COOKIE", "0123456789")),
                new Gson().toJson(produceCookieObject(URI.create("http://www.example.com/"), "www.example.com", "/", "ad", newRandomCookieValue())),
            };
            URI uri;
            try {
                URIBuilder uriBuilder = new URIBuilder(URI.create("chrome-extension://" + EXTENSION_ID + "/manage.html"));
                for (String cookieJson : cookieJsons) {
                    uriBuilder.addParameter("import", cookieJson);
                }
                uri = uriBuilder.build();
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
            System.out.println("visiting URI:");
            System.out.println(uri);
            driver.get(uri.toString());
            WebElement outputElement = new WebDriverWait(driver, 3)
                    .until(new java.util.function.Function<WebDriver, WebElement>(){
                        @Nullable
                        @Override
                        public WebElement apply(WebDriver input) {
                            throw new UnsupportedOperationException("deprecated; use byOutputStatus instance method of CookieImplanter");
                        }
                    });
            String outputJson = outputElement.getText();
            System.out.format("output json:%n%s%n", StringUtils.abbreviate(new Gson().toJson(new JsonParser().parse(outputJson)), 256));
            if (outputJson.trim().isEmpty()) {
                System.out.println("no output?");
                return false;
            }
            JsonObject output = new JsonParser().parse(outputJson).getAsJsonObject();
            if (output.has("status") && output.get("status").getAsString().equals("all_imports_processed")) {
                JsonArray imports = output.get("imports").getAsJsonArray();
                int numFailures = 0;
                for (JsonElement element : imports) {
                    JsonObject elementObj = element.getAsJsonObject();
                    System.out.format("%s %s %s%n", elementObj.get("index"), elementObj.get("success"), elementObj.get("message"));
                    if (!elementObj.get("success").getAsBoolean()) {
                        numFailures++;
                    }
                }
                System.out.format("%d cookie imports failed%n", numFailures);
                if (imports.size() == 0) {
                    System.out.println("no imported cookies in output");
                }
                return imports.size() > 0 && numFailures == 0;
            } else {
                System.out.println("output status: " + output.get("status"));
                return false;
            }
        }

        @Override
        public boolean shouldRemainOpen(Boolean allSuccesses, WebDriver driver) {
            return allSuccesses;
        }
    }

    private static <T> void main(Doer<T> doer) throws Exception {
        UnitTests.setupRecommendedChromeDriver();
        File tmpDir = new File(System.getProperty("user.dir"), "target");
        File userDataDir = Files.createTempDirectory(tmpDir.toPath(), "chrome").toFile();
        try {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--user-data-dir=" + userDataDir);
            doer.prepare(tmpDir, options);
            ChromeDriver driver = new ChromeDriver(options);
            try {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                    T stuff = doer.doStuff(driver);
                    if (doer.shouldRemainOpen(stuff, driver)) {
                        while (true) {
                            System.out.print("URL: ");
                            String line = reader.readLine();
                            if (line == null) {
                                break;
                            }
                            if (!line.isEmpty()) {
                                if ("exit".equalsIgnoreCase(line.trim())) {
                                    break;
                                }
                                driver.get(line.trim());
                            }
                        }
                    }
                }
            } finally {
                driver.quit();
            }

            if (userDataDir.isDirectory()) {
                Collection<File> tmpFiles = FileUtils.listFiles(userDataDir, null, true);
                tmpFiles.forEach(System.out::println);
            } else {
                System.err.format("not a directory: %s%n", userDataDir);
            }
        } finally {
            if (!doer.isRetainUserDataDir()) {
                FileUtils.deleteDirectory(userDataDir);
            }
        }
    }

    private static void dumpEchoes(String preCookieGet, String postCookieGet) {
        System.out.println("pre-cookie implant:");
        System.out.println(preCookieGet);
        System.out.println();
        System.out.println("post-cookie implant:");
        System.out.println(postCookieGet);
        System.out.println();
    }
}
