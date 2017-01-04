package com.github.mike10004.seleniumhelp;

import com.github.mike10004.seleniumhelp.HarAnalysis.CookieCollection;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.math.LongMath;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarTimings;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;

public class HarAnalysisTest {

    @Test
    public void CookieCollection_makeUltimateCookieList_simple() throws Exception {
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
    public void CookieCollection_makeUltimateCookieList() throws Exception {
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
    public void CookieCollection_sortHarEntries() {
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
}