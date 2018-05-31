package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ArrayListMultimap;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;

public class MultimapCookieCollectionTest {

    @Test
    public void makeUltimateCookieList_simple() throws Exception {
        System.out.println("CookieCollection_makeUltimateCookieList_simple");
        Date t1Start = new Date(1483557811893L);
        Date t1Finish = DateUtils.addMilliseconds(t1Start, 400);
        Date t2Start = DateUtils.addMilliseconds(t1Start, -100);
        Date t2Finish = DateUtils.addMilliseconds(t1Finish, 150);
        checkState(t2Finish.after(t1Finish));
        long t1Duration = computeDuration(t1Start, t1Finish), t2Duration = computeDuration(t2Start, t2Finish);
        List<DeserializableCookie> mm = new ArrayList<>();
        mm.add(createCookie("example.com", "/", "foo", "bad", t1Start, t1Duration));
        mm.add(createCookie("example.com", "/", "foo", "good", t2Start, t2Duration));
        CookieCollection cc = MultimapCookieCollection.build(mm);
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
        List<DeserializableCookie> mm = new ArrayList<>();
        mm.add(createCookie("example.com", "/", "foo", "bad", t1Start, t1Duration));
        mm.add(createCookie("bar.com", "/", "foo", "good", t1Start, t1Duration));
        mm.add(createCookie("example.com", "/", "foo", "good", t2Start, t2Duration));
        mm.add(createCookie("bar.com", "/gaw", "foo", "good", t3Start, t3Duration));
        mm.add(createCookie("baz.com", "/", "hello", "good", t3Start, t3Duration));
        CookieCollection cc = MultimapCookieCollection.build(mm);
        List<DeserializableCookie> ultimates = cc.makeUltimateCookieList();
        assertEquals("num ultimates", 4, ultimates.size());
        Set<String> values = ultimates.stream().map(DeserializableCookie::getValue).collect(Collectors.toSet());
        Set<String> expectedValues = ImmutableSet.of("good");
        assertEquals("values", expectedValues, values);
    }

    private static DeserializableCookie createCookie(String domain, String path, String name, String value, Date start, long duration) {
        Instant creationDate = start.toInstant().plus(duration, ChronoUnit.MILLIS);
        DeserializableCookie d = DeserializableCookie.builder(name, value)
                .domain(domain)
                .creationDate(Date.from(creationDate))
                .path(path).build();
        return d;
    }

    private static long computeDuration(Date from, Date to) {
        return LongMath.checkedSubtract(to.getTime(), from.getTime());
    }


}