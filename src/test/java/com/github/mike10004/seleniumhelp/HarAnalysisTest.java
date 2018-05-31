package com.github.mike10004.seleniumhelp;

import com.google.gson.Gson;
import net.lightbody.bmp.core.har.Har;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class HarAnalysisTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void findCookies() throws Exception {
        File harFile = new File("/tmp/for-testing.har");
        Har har = readHar(harFile);
        List<DeserializableCookie> cookies = HarAnalysis.of(har).findCookies().makeUltimateCookieList();
        DeserializableCookie cookie = cookies.stream().filter(c -> TARGET_COOKIE_NAME.equalsIgnoreCase(c.getName())).findFirst().orElse(null);
        assertNotNull("target", cookie);
        System.out.println(cookie);
        assertTrue("has expiry", cookie.isPersistent());
    }

    private static final String TARGET_COOKIE_NAME = "sourceforge";

    private static Har readHar(File harFile) throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream(harFile), UTF_8)) {
            return new Gson().fromJson(reader, Har.class);
        }
    }
}