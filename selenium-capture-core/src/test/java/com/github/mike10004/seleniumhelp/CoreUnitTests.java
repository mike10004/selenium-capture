package com.github.mike10004.seleniumhelp;

import io.github.mike10004.seleniumcapture.testing.UnitTests;

import java.io.File;

/**
 * Static constants and utility methods to assist with tests.
 */
public class CoreUnitTests {

//    static final String IGNORE_BECAUSE_UPGRADE_INSECURE_REQUESTS_UNAVOIDABLE =
//            "We would like to test HTTPS but requests are sent with header " +
//                    "'Upgrade-Insecure-Requests: 0' and there's no way to disable " +
//                    "that; in some cases, HTTP connections can only be tested " +
//                    "locally (on localhost)";
//    private static final String SETTING_OPENSSL_TESTS_SKIP = "openssl.skip";
    private static final String SETTING_OPENSSL_EXECUTABLE_PATH = "openssl.executable.path";
//    private static final String SETTING_DEBUG_ENVIRONMENT = "environment.debug";
//
//    private CoreUnitTests() {}
//
//    @Deprecated
//    private static final SettingSet Settings = UnitTests.Settings;
//
//
public static ExecutableConfig makeOpensslConfig() {
    String path = UnitTests.Settings.get(SETTING_OPENSSL_EXECUTABLE_PATH);
    if (path != null) {
        File file = new File(path);
        System.out.format("using openssl executable at %s%n", file);
        return ExecutableConfig.byPathOnly(file);
    }
    return ExecutableConfig.byNameOnly("openssl");
}

    public static ExecutableConfig makeKeytoolConfig() {
        return ExecutableConfig.byNameOnly("keytool");
    }
//
//    public static boolean isSkipOpensslTests() {
//        return Settings.get(SETTING_OPENSSL_TESTS_SKIP, false);
//    }
//
//    public static String removeHtmlWrapping(String html) {
//        org.jsoup.nodes.Document doc = Jsoup.parse(html);
//        return doc.text();
//    }
//
//    private static final Duration SHOW_BROWSER_FLAG_FILE_LIFETIME = Duration.ofHours(24);
//
//    private static boolean isShowBrowserFlagFilePresentAndNotExpired() {
//        File flagFile = new File(System.getProperty("user.dir"), ".show-browser-window");
//        if (flagFile.isFile()) {
//            @Nullable Instant lastModified = null;
//            try {
//                lastModified = getFileLastModified(flagFile);
//            } catch (IOException ignore) {
//            }
//            if (lastModified != null) {
//                Instant deadline = Instant.now().minus(SHOW_BROWSER_FLAG_FILE_LIFETIME);
//                return lastModified.isAfter(deadline);
//            }
//        }
//        return false;
//    }
//
//    private static Instant getFileLastModified(File file) throws IOException {
//        BasicFileAttributeView attrView = java.nio.file.Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class);
//        BasicFileAttributes attrs = attrView.readAttributes();
//        FileTime lastModifiedTime = attrs.lastModifiedTime();
//        long epochMilli = lastModifiedTime.toMillis();
//        return Instant.ofEpochMilli(epochMilli);
//    }
//
//    public static boolean isShowBrowserWindowEnabled() {
//        if (isShowBrowserFlagFilePresentAndNotExpired()) {
//            return true;
//        }
//        return Settings.get("showBrowserWindow", false);
//    }
//
//    public static XvfbRule.Builder xvfbRuleBuilder() {
//        return XvfbRule.builder()
//                .disabled(com.github.mike10004.seleniumhelp.UnitTests::isShowBrowserWindowEnabled);
//    }
//
//    @Nullable
//    public static String getPageSourceOrNull(WebDriver driver) {
//        return tryGet(driver::getPageSource, null, ignore ->{});
//    }
//
//    @Nullable
//    public static String getDomOrNull(WebDriver driver) {
//        return tryGet(() -> {
//            return (String) ((JavascriptExecutor)driver).executeScript("return document.getElementsByTagName('html')[0].innerHTML");
//        }, null, ignore -> {});
//    }
//
//    @SuppressWarnings("SameParameterValue")
//    private static <T> T tryGet(Supplier<T> getter, T defaultValue, Consumer<? super RuntimeException> errorListener) {
//        try {
//            return getter.get();
//        } catch (RuntimeException e) {
//            errorListener.accept(e);
//        }
//        return defaultValue;
//    }
//
//    public static void dumpState(WebDriver driver, PrintStream err) {
//        err.println("============== +PAGE URL  ============");
//        err.println(tryGet(driver::getCurrentUrl, null, ignore -> {}));
//        err.println("============== -PAGE URL ============");
//        err.println("============== +PAGE SOURCE ============");
//        err.println(getPageSourceOrNull(driver));
//        err.println("============== -PAGE SOURCE ============");
//        err.println("============== +PAGE DOM ============");
//        err.println(getDomOrNull(driver));
//        err.println("============== -PAGE DOM ============");
//    }
//
//    public static Date truncateToSeconds(long millisSinceEpoch) {
//        return DateUtils.truncate(new Date(millisSinceEpoch), Calendar.SECOND);
//    }

}
