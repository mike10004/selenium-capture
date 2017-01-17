package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.math.LongMath;
import com.google.common.net.HttpHeaders;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import net.lightbody.bmp.core.har.HarRequest;
import net.lightbody.bmp.core.har.HarResponse;
import net.lightbody.bmp.core.har.HarTimings;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;

public class CookieCollectionTest {

    @Test
    public void makeUltimateCookieList_simple() throws Exception {
        System.out.println("CookieCollection_makeUltimateCookieList_simple");
        Date t1Start = new Date(1483557811893L);
        Date t1Finish = DateUtils.addMilliseconds(t1Start, 400);
        Date t2Start = DateUtils.addMilliseconds(t1Start, -100);
        Date t2Finish = DateUtils.addMilliseconds(t1Finish, 150);
        checkState(t2Finish.after(t1Finish));
        long t1Duration = computeDuration(t1Start, t1Finish), t2Duration = computeDuration(t2Start, t2Finish);
        HarEntry h1 = createHarEntry(t1Start, t1Duration), h2 = createHarEntry(t2Start, t2Duration);
        long response1 = CookieCollection.getEntryResponseInstant(h1), response2 = CookieCollection.getEntryResponseInstant(h2);
        checkState(response2 > response1, "expect response1 %s < %s response2 (from %s, %s)", response1, response2, describeTime(h1), describeTime(h2));
        Multimap<HarEntry, DeserializableCookie> mm = ArrayListMultimap.create();
        mm.put(h1, createCookie("example.com", "/", "foo", "bad"));
        mm.put(h2, createCookie("example.com", "/", "foo", "good"));
        checkState(mm.keySet().size() == 2);
        CookieCollection cc = new CookieCollection(mm);
        List<DeserializableCookie> ultimates = cc.makeUltimateCookieList();
        assertEquals("num ultimates", 1, ultimates.size());
        Set<String> values = ultimates.stream().map(DeserializableCookie::getValue).collect(Collectors.toSet());
        Set<String> expectedValues = ImmutableSet.of("good");
        assertEquals("values", expectedValues, values);
    }

    @Test
    public void makeUltimateCookieList() throws Exception {
        Date t1Start = new Date(1483557811893L);
        Date t1Finish = DateUtils.addMilliseconds(t1Start, 400);
        Date t2Start = DateUtils.addMilliseconds(t1Start, -100);
        Date t2Finish = DateUtils.addMilliseconds(t1Finish, 150);
        Date t3Start = DateUtils.addMilliseconds(t2Finish, 200);
        Date t3Finish = DateUtils.addMilliseconds(t3Start, 300);
        checkState(t2Finish.after(t1Finish) && t3Finish.after(t2Finish));
        long t1Duration = computeDuration(t1Start, t1Finish), t2Duration = computeDuration(t2Start, t2Finish), t3Duration = computeDuration(t3Start, t3Finish);
        HarEntry h1 = createHarEntry(t1Start, t1Duration), h2 = createHarEntry(t2Start, t2Duration), h3 = createHarEntry(t3Start, t3Duration);
        Multimap<HarEntry, DeserializableCookie> mm = ArrayListMultimap.create();
        mm.put(h1, createCookie("example.com", "/", "foo", "bad"));
        mm.put(h1, createCookie("bar.com", "/", "foo", "good"));
        mm.put(h2, createCookie("example.com", "/", "foo", "good"));
        mm.put(h3, createCookie("bar.com", "/gaw", "foo", "good"));
        mm.put(h3, createCookie("baz.com", "/", "hello", "good"));
        CookieCollection cc = new CookieCollection(mm);
        List<DeserializableCookie> ultimates = cc.makeUltimateCookieList();
        assertEquals("num ultimates", 4, ultimates.size());
        Set<String> values = ultimates.stream().map(DeserializableCookie::getValue).collect(Collectors.toSet());
        Set<String> expectedValues = ImmutableSet.of("good");
        assertEquals("values", expectedValues, values);
    }

    @Test
    public void sortHarEntries() {
        Date t1Start = new Date(1483557811893L);
        Date t1Finish = DateUtils.addMilliseconds(t1Start, 400);
        Date t2Start = DateUtils.addMilliseconds(t1Start, -100);
        Date t2Finish = DateUtils.addMilliseconds(t1Finish, 150);
        Date t3Start = DateUtils.addMilliseconds(t2Finish, 200);
        Date t3Finish = DateUtils.addMilliseconds(t3Start, 300);
        checkState(t2Finish.after(t1Finish) && t3Finish.after(t2Finish));
        long t1Duration = computeDuration(t1Start, t2Finish), t2Duration = computeDuration(t2Start, t2Finish), t3Duration = computeDuration(t3Start, t3Finish);
        HarEntry h1 = createHarEntry(t1Start, t1Duration), h2 = createHarEntry(t2Start, t2Duration), h3 = createHarEntry(t3Start, t3Duration);
        assertEquals("ordering", ImmutableList.of(h1, h2, h3), CookieCollection.sortHarEntriesByResponseInstant(Stream.of(h1, h2, h3)).collect(Collectors.toList()));
    }

    private static DeserializableCookie createCookie(String domain, String path, String name, String value) {
        DeserializableCookie d = new DeserializableCookie();
        d.setDomain(domain);
        d.setName(name);
        d.setPath(path);
        d.setValue(value);
        return d;
    }

    private static long computeDuration(Date from, Date to) {
        return LongMath.checkedSubtract(to.getTime(), from.getTime());
    }

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

    private static String describeTime(HarEntry h) {
        return MoreObjects.toStringHelper(h)
                .add("start", h.getStartedDateTime().getTime())
                .add("duration", h.getTime())
                .toString();
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
            CookieCollection cookies = CookieCollection.build(Stream.of(entry));
            List<DeserializableCookie> cookieList = cookies.makeUltimateCookieList();
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
}