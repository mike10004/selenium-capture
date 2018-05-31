package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import net.lightbody.bmp.core.har.HarRequest;
import net.lightbody.bmp.core.har.HarResponse;
import net.lightbody.bmp.core.har.HarTimings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HarAnalysisTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void makeCookieFromEntry() throws Exception {
        HarEntry entry = createHarEntry(new Date(), 10, ImmutableMap.of("foo", "bar"));
        List<DeserializableCookie> cookieList = HarAnalysis.makeCookiesFromEntry(SetCookieHeaderParser.create(), entry);
        assertEquals("cookie count", 1, cookieList.size());
    }


    @Test
    public void makeIdenticalCookies() throws Exception {
        Date entryStart = new Date();
        long duration = 10;
        Map<String, String> data = ImmutableMap.of("foo", "bar");
        HarEntry entry1 = createHarEntry(entryStart, duration, data);
        DeserializableCookie cookie1 = HarAnalysis.makeCookiesFromEntry(SetCookieHeaderParser.create(), entry1).iterator().next();
        Thread.sleep(1000);
        HarEntry entry2 = createHarEntry(entryStart, duration, data);
        checkState(isEqual(entry1, entry2), "har entries are not equal");
        DeserializableCookie cookie2 = HarAnalysis.makeCookiesFromEntry(SetCookieHeaderParser.create(), entry2).iterator().next();
        assertEquals("cookies", cookie1, cookie2);
    }

    private boolean isEqual(HarEntry entry1, HarEntry entry2) {
        Gson gson = new Gson();
        String json1 = gson.toJson(entry1);
        String json2 = gson.toJson(entry2);
        return json1.equals(json2);
    }

    @SuppressWarnings("SameParameterValue")
    private static HarEntry createHarEntry(Date start, long duration, Map<String, String> cookieNamesAndValues) {
        checkArgument(duration >= 6, "duration must be at least 6 milliseconds");
        HarEntry entry = new HarEntry();
        entry.setStartedDateTime(start);
        HarTimings timings = new HarTimings();
        timings.setBlocked(1);
        timings.setConnect(1);
        timings.setDns(1);
        timings.setReceive(1);
        timings.setSend(1);
        timings.setSsl(1);
        timings.setWait(duration - 6);
        entry.setTimings(timings);
        HarRequest request = new HarRequest();
        request.setUrl("https://www.example.com/");
        entry.setRequest(request);
        HarResponse response = new HarResponse();
        entry.setResponse(response);
        List<HarNameValuePair> headers = response.getHeaders();
        cookieNamesAndValues.forEach((name, value) -> {
            headers.add(new HarNameValuePair(HttpHeaders.SET_COOKIE, String.format("%s=%s", name, value)));
        });
        return entry;
    }
}