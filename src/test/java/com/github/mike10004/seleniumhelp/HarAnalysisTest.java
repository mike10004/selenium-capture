package com.github.mike10004.seleniumhelp;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import net.lightbody.bmp.core.har.HarRequest;
import net.lightbody.bmp.core.har.HarResponse;
import net.lightbody.bmp.core.har.HarTimings;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HarAnalysisTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void findCookies() throws Exception {
        File harFile = new File("/tmp/for-testing.har");
        Har har = readHar(harFile);
        MultimapCookieCollection collection = (MultimapCookieCollection) HarAnalysis.of(har).findCookies();
        System.out.format("%s received%n", collection.getAllReceived().size());
        collection.getAllReceived().forEach(cookie -> {
            System.out.format("%s %s -> %s (created %s, expires %s)%n", cookie.getBestDomainProperty(), cookie.getName(), StringUtils.abbreviate(cookie.getValue(), 32), cookie.getCreationDate().getTime(), cookie.getExpiryDate());
        });
        List<DeserializableCookie> cookies = collection.makeUltimateCookieList();
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

    @Test
    public void makeCookiesFromEntry() throws IOException {
        Charset charset = StandardCharsets.UTF_8;
        ByteArrayOutputStream bucket = new ByteArrayOutputStream(256);
        PrintStream stdout = System.out;
        try (PrintStream bucketOut = new PrintStream(bucket, false, charset.name())){
            System.setOut(bucketOut);
            HarEntry entry = createHarEntry(new Date(), 10);
            HarRequest request = new HarRequest();
            request.setUrl("https://www.example.com/");
            entry.setRequest(request);
            HarResponse response = new HarResponse();
            entry.setResponse(response);
            response.getHeaders().add(new HarNameValuePair(HttpHeaders.SET_COOKIE, "foo=bar"));
            List<DeserializableCookie> cookieList = HarAnalysis.makeCookiesFromEntry(FlexibleCookieSpec.getDefault(), entry);
            assertEquals("cookie count", 1, cookieList.size());
        } finally {
            System.setOut(stdout);
        }
        String bucketContents = new String(bucket.toByteArray(), charset);
        if (!bucketContents.isEmpty()) {
            System.out.println("printed on stdout during CookieCollection.build or CookieCollection.makeUltimateCookieList:");
            System.out.println(bucketContents);
        }
        assertEquals("bucket should be empty", 0, bucketContents.length());
    }


    @SuppressWarnings("SameParameterValue")
    private static HarEntry createHarEntry(Date start, long duration) {
        checkArgument(duration >= 6, "duration must be at least 6 milliseconds");
        HarEntry he = new HarEntry();
        he.setStartedDateTime(start);
        HarTimings timings = new HarTimings();
        timings.setBlocked(1);
        timings.setConnect(1);
        timings.setDns(1);
        timings.setReceive(1);
        timings.setSend(1);
        timings.setSsl(1);
        timings.setWait(duration - 6);
        he.setTimings(timings);
        return he;
    }
}