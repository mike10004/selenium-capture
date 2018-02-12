package com.github.mike10004.seleniumhelp;

import com.github.mike10004.seleniumhelp.FirefoxWebDriverFactory.FirefoxProfileFolderAction;
import com.github.mike10004.xvfbmanager.XvfbController;
import com.google.common.collect.Multimap;
import com.google.gson.GsonBuilder;
import net.lightbody.bmp.core.har.Har;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FirefoxCookieUsageTest extends CookieUsageTestBase {

    @ClassRule
    public static WebDriverManagerRule geckodriverSetupRule = WebDriverManagerRule.geckodriver();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ProfileFolderTracker tracker;

    @Before
    public void createProfileFolderTracker() {
        tracker = new ProfileFolderTracker();
    }

    @Override
    protected WebDriverFactory createCookielessWebDriverFactory(XvfbController xvfbController) throws IOException {
        return FirefoxWebDriverFactory.builder()
                .binary(UnitTests.createFirefoxBinarySupplier())
                .environment(xvfbController::newEnvironment)
                .profileFolderAction(tracker)
                .build();
    }

    @Override
    protected WebDriverFactory createCookiefulWebDriverFactory(XvfbController xvfbController, List<DeserializableCookie> cookiesSetByServer) throws IOException {
        return FirefoxWebDriverFactory.builder()
                .binary(UnitTests.createFirefoxBinarySupplier())
                .environment(xvfbController::newEnvironment)
                .scratchDir(temporaryFolder.getRoot().toPath())
                .cookies(cookiesSetByServer)
                .profileFolderAction(tracker)
                .build();
    }

    private class ProfileFolderTracker implements FirefoxProfileFolderAction {

        private final AtomicInteger invocations = new AtomicInteger(0);

        @Override
        public void perform(File profileDir) {
            invocations.incrementAndGet();
            System.out.format("firefox profile folder: %s%n", profileDir.getAbsolutePath());
        }
    }

    @Test
    public void testCookieUsage() throws Exception {
        exerciseCookieCapabilities();
    }

    @Override
    protected void browsingFinished(Multimap<String, String> cookieHeaderValues, URL cookieGetUrl, Har har, List<DeserializableCookie> cookiesSetByServer) {
        // // In theory, FirefoxProfile.layoutOnDisk is only invoked once per FirefoxDriver instantiation.
        // // In practice it gets invoked once when cleaning the options object and once when starting
        // // the selenium wire session.
        // assertEquals("num ProfileFolderTracker.perform invocations", 2, tracker.invocations.get());
        super.browsingFinished(cookieHeaderValues, cookieGetUrl, har, cookiesSetByServer);
    }

    @Override
    protected void checkOurCookies(Iterable<DeserializableCookie> cookies) {
        System.out.println("received cookies:");
        new GsonBuilder().setPrettyPrinting().create().toJson(cookies, System.out);
        System.out.println();
        super.checkOurCookies(cookies);
    }
}
