package io.github.mike10004.seleniumcapture.firefox;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import io.github.mike10004.nanochamp.server.NanoControl;
import io.github.mike10004.nanochamp.server.NanoResponse;
import io.github.mike10004.nanochamp.server.NanoServer;
import io.github.mike10004.seleniumcapture.WebdrivingConfig;
import io.github.mike10004.seleniumcapture.WebdrivingSession;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.Subprocess;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystemNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test that confirms that the properties of the cookies we set
 * are correctly translated into the sqlite database.
 */
public class FirefoxCookieStorageDbTest {

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final int MAX_AGE = 60 * 60 * 24; // 1 day

    private static String cookieHeader(String name, String value, String sameSite) {
        return String.format("%s=%s; domain=localhost; SameSite=%s; Max-Age=%s; HttpOnly", name, value, sameSite, MAX_AGE);
    }

    private static final boolean interactive = false;

    @Test
    public void testCookieStorageInFirefoxDb() throws Exception {
        Assume.assumeTrue("this test is only compatible with Unixlike",
                !Platform.getCurrent().equals(Platform.WINDOWS));
        File tmpdir = temporaryFolder.newFolder("mztmp");
        File pdest = temporaryFolder.newFolder("saved");
        FirefoxTestParameter ftp = new FirefoxTestParameter(true);
        ftp.doDriverManagerSetup();
        String laxName = "sc_ss_lax", noneName = "sc_ss_non", strictName = "sc_ss_str";
        NanoServer server = NanoServer.builder()
                .getPath("/exit", session -> {
                    return NanoResponse.status(200).plainTextUtf8("exited");
                })
                .getPath("/", session -> {
                    return NanoResponse.status(200)
                            .header("Set-Cookie", cookieHeader(laxName, "foo", "Lax"))
                            .header("Set-Cookie", cookieHeader(noneName, "bar", "None"))
                            .header("Set-Cookie", cookieHeader(strictName, "baz", "Strict"))
                            .htmlUtf8("<!DOCTYPE html><html><body><a href=\"/exit\">exit</a></body></html>");
                })
                .build();
        String tmpdirPath = tmpdir.getAbsolutePath();
        Map<String, String> env = ImmutableMap.<String, String>builder()
                .put("TMP", tmpdirPath)
                .put("TMPDIR", tmpdirPath)
                .put("TEMP", tmpdirPath)
                .put("TEMPDIR", tmpdirPath)
                .build();
        File actualProfileDir;
        long cookieReceiptMoment = 0;
        try (NanoControl ctrl = server.startServer()) {
//            FirefoxWebDriverFactory webDriverFactory = ftp.buildWebDriverFactory(xvfb)
            FirefoxWebDriverFactory webDriverFactory = FirefoxWebDriverFactory.builder()
                    .disableTrackingProtection()
                    .disableRemoteSettings()
                    .environment(env)
                    .configure(o -> o.setHeadless(!interactive))
                    .build();
            try (WebdrivingSession session = webDriverFactory.startWebdriving(WebdrivingConfig.nonCapturing())) {
                WebDriver webdriver = session.getWebDriver();
                webdriver.get(ctrl.baseUri().toString());
                cookieReceiptMoment = System.currentTimeMillis();
                if (interactive) {
                    new WebDriverWait(webdriver, Duration.ofSeconds(3600))
                            .until(ExpectedConditions.urlContains("/exit"));
                } else {
                    Thread.sleep(1000); // allow some time for async cookie operations
                }
                actualProfileDir = getActualRustProfileFolder(tmpdir);
                killFirefoxProcessExternally("firefox", new String[]{"-profile", actualProfileDir.getAbsolutePath()});
                System.out.println("killed firefox process");
                copyFolder(actualProfileDir, pdest.getAbsolutePath());
            } catch (WebDriverException e) {
                System.out.format("session ended with %s%n", e.getClass().getName());
            }
            System.out.println("quitted");
        }
        Sqlite3GenericExporter exporter = new Sqlite3GenericExporter(Sqlite3Runner.createDefault());
        File cookiesDbFile = new File(pdest, "cookies.sqlite");
        if (!cookiesDbFile.exists()) {
            Collection<File> filesInSavedDir = FileUtils.listFiles(pdest, null, false);
            filesInSavedDir.forEach(System.out::println);
        }
        System.out.println("found cookies db: " + cookiesDbFile.getAbsolutePath());
        assertTrue("expect " + cookiesDbFile + " exists", cookiesDbFile.isFile());
        List<Map<String, String>> records = exporter.dumpRows("moz_cookies", cookiesDbFile);
        records.forEach(cookie -> {
            System.out.format("%s=%s %s=%s%n", cookie.get("name"), cookie.get("value"),
                    Firefox91CookieImporter.COL_SAMESITE, cookie.get(Firefox91CookieImporter.COL_SAMESITE));
        });
        // allow for the possibility that some cookie gets inserted not by us (e.g. by a phone home)
        assertTrue("num cookies in " + records, records.size() >= 3);
        Instant expectedExpiry = Instant.ofEpochMilli(cookieReceiptMoment).plus(Duration.ofSeconds(MAX_AGE));
        Duration delta = Duration.ofMinutes(1);
        Map<String, String> laxCookie = findCookieRecordByCookieName(records, laxName);
        Map<String, String> strictCookie = findCookieRecordByCookieName(records, strictName);
        Map<String, String> noneCookie = findCookieRecordByCookieName(records, noneName);
        checkCookie(laxCookie, expectedExpiry, delta);
        assertCookieCorrect(laxCookie, Firefox91CookieImporter.COL_SAMESITE, "1");
        checkCookie(strictCookie, expectedExpiry, delta);
        assertCookieCorrect(strictCookie, Firefox91CookieImporter.COL_SAMESITE, "2");
        checkCookie(noneCookie, expectedExpiry, delta);
        assertCookieCorrect(noneCookie, Firefox91CookieImporter.COL_SAMESITE, "0");
    }

    private void checkCookie(Map<String, String> laxCookie, Instant expectedExpiry, Duration delta) {
        assertCookieCorrect(laxCookie, Firefox91CookieImporter.COL_HOST, "localhost", Firefox91CookieImporter.COL_IS_HTTP_ONLY, "1");
        assertTrue("expiry of " + laxCookie, isWithin(expectedExpiry, getTimestamp(laxCookie, Firefox91CookieImporter.COL_EXPIRY, TimeUnit.SECONDS), delta));
    }

    private static boolean isWithin(Instant reference, Instant query, Duration window) {
        Duration between = Duration.between(reference, query);
        long betweenMillis = Math.abs(between.toMillis());
        long windowMillis = window.toMillis();
        return betweenMillis < windowMillis;
    }

    @SuppressWarnings("SameParameterValue")
    private static Instant getTimestamp(Map<String, String> record, String field, TimeUnit unit) {
        String valueStr = record.get(field);
        if (valueStr == null) {
            throw new IllegalArgumentException("field " + field + " not present in " + record);
        }
        long millis = unit.toMillis(Long.parseLong(valueStr));
        return Instant.ofEpochMilli(millis);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertCookieCorrect(Map<String, String> cookieRecord, String name1, String value1, String...moreNamesAndValues) {
        Map<String, String> required = new LinkedHashMap<>();
        required.put(name1, value1);
        for (int i = 0; i < moreNamesAndValues.length; i+=2) {
            required.put(moreNamesAndValues[i], moreNamesAndValues[i+1]);
        }
        required.forEach((key, value) -> {
            assertEquals("key " + key + " in " + cookieRecord, value, cookieRecord.get(key));
        });
    }

    private static Map<String, String> findCookieRecordByCookieName(List<Map<String, String>> records, String cookieName) {
        return records.stream()
                .filter(record -> {
                    return cookieName.equals(record.get(Firefox91CookieImporter.COL_NAME));
                }).findFirst().orElseThrow(() -> new IllegalArgumentException("no cookie by name " + cookieName));
    }

    private static File getActualRustProfileFolder(File tmpdir) throws IOException {
        String profileDirBasename = Arrays.stream(requireNonNull(tmpdir.list(), "expect directory: " + tmpdir))
                .filter(basename -> basename.startsWith("rust_mozprofile"))
                .findFirst()
                .orElseThrow(() -> new FileSystemNotFoundException("anything starting with rust_mozprofile in " + tmpdir));
        File profileDir = new File(tmpdir, profileDirBasename);
        if (!profileDir.isDirectory()) {
            throw new FileNotFoundException(profileDir.getAbsolutePath());
        }
        return profileDir;
    }

    private static <T> int findArray(T[] array, T[] subArray)
    {
        return Collections.indexOfSubList(Arrays.asList(array), Arrays.asList(subArray));
    }

    private static boolean isBasenamesEqual(String path1, String path2) {
        return (path1.isEmpty() && path2.isEmpty()) || FilenameUtils.getName(path1).equals(FilenameUtils.getName(path2));
    }

    @SuppressWarnings("SameParameterValue")
    private static void killFirefoxProcessExternally(String command, String[] requiredArgSubsequence) throws InterruptedException, java.util.concurrent.TimeoutException, ExecutionException {
        List<ProcessHandle> handles = ProcessHandle.allProcesses()
                .filter(ph -> {
                    String[] args = ph.info().arguments().orElse(new String[0]);
                    return isBasenamesEqual(command, ph.info().command().orElse(""))
                            && findArray(args, requiredArgSubsequence) >= 0;
                }).collect(Collectors.toList());
        System.out.format("%s processes match profile dir%n", handles.size());
        if (handles.isEmpty()) {
            throw new RuntimeException("could not find correct firefox process");
        }
        if (handles.size() > 1) {
            throw new RuntimeException("too many processes match profile dir: " + handles);
        }
        ProcessHandle process = handles.get(0);
        long pid = process.pid();
        System.out.format("firefox process has pid %s with command line %s%n", pid, process.info().commandLine().orElse(null));
        sendSignal(pid, "SIGTERM");
        // I suppose there's a race condition here where another process with this id might
        // be created, but my understanding is that is very unlikely
        ProcessHandle reprocess = ProcessHandle.allProcesses()
                .filter(p -> {
                    return pid == p.pid() && isBasenamesEqual(command, p.info().command().orElse(""));
                }).findFirst().orElse(null);
        if (reprocess != null) {
            System.out.format("waiting for process %s to die%n", pid);
            try {
                reprocess.onExit().get(5, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                System.out.println(describe(reprocess));
                throw new AssertionError("process did not die");
            }
        } // good if null (it no longer exists)
    }

    private static String describe(ProcessHandle p) {
        return MoreObjects.toStringHelper(p)
                .add("command", p.info().command())
                .add("args", Arrays.toString(p.info().arguments().orElse(new String[0])))
                .add("pid", p.pid())
                .add("supportsNormalTermination", p.supportsNormalTermination())
                .add("alive", p.isAlive())
                .toString();
    }

    private static void sendSignal(long pid, @SuppressWarnings("SameParameterValue") String signalName) throws InterruptedException, TimeoutException {
        ProcessResult<String, String> result;
        try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
            result = Subprocess.running("kill")
                    .arg("-" + signalName)
                    .arg(String.valueOf(pid))
                    .build()
                    .launcher(processTracker)
                    .outputStrings(Charset.defaultCharset())
                    .launch()
                    .await(5, TimeUnit.SECONDS);
        }
        assertEquals("expect kill to return 0", 0, result.exitCode());
    }

    @SuppressWarnings("SameParameterValue")
    private void copyFolder(File srcDir, String dstPathname) throws IOException {
        File dstdir = new File(dstPathname);
        FileUtils.deleteDirectory(dstdir);
        FileUtils.copyDirectory(srcDir, dstdir);
        System.out.format("%s is saved profile folder%n", dstdir.getAbsolutePath());
    }
}
