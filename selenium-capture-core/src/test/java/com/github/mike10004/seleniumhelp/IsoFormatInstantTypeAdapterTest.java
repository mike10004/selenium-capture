package com.github.mike10004.seleniumhelp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class IsoFormatInstantTypeAdapterTest {

    @Test
    public void write() throws Exception {
        Instant now = Instant.now();
        String nowJson = gson().toJson(now);
        String nowStr = new JsonParser().parse(nowJson).getAsString();
        System.out.println("write: " + nowStr);
        Instant deserialized = Instant.from(IsoFormatInstantTypeAdapter.getDefaultOutputFormatter().parse(nowStr));
        assertEquals("deserialized", now, deserialized);
    }

    @Test
    public void read_iso_upToSeconds() throws Exception {

        String instantStr = "2016-09-15T18:33:58+00:00";
        String json = new Gson().toJson(instantStr);
        Instant deserialized = gson().fromJson(json, Instant.class);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(deserialized.toEpochMilli());
        assertEquals(2016, cal.get(Calendar.YEAR));
        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(33, cal.get(Calendar.MINUTE));
        assertEquals(58, cal.get(Calendar.SECOND));
    }

    @Test
    public void read_iso_upToMilliseconds() throws Exception {

        String instantStr = "2016-09-15T18:33:58.012+00:00";
        String json = new Gson().toJson(instantStr);
        Instant deserialized = gson().fromJson(json, Instant.class);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(deserialized.toEpochMilli());
        assertEquals(12, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void read_holder() throws Exception {
        Holder expected = new Holder(Instant.now());
        JsonElement holderJson = gson().toJsonTree(expected);
        Holder actual = gson().fromJson(holderJson, Holder.class);
        assertEquals("holder", expected, actual);

    }

    private static class Holder {

        public final Instant instant;

        public Holder(Instant instant) {
            this.instant = instant;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Holder)) return false;

            Holder holder = (Holder) o;

            return instant != null ? instant.equals(holder.instant) : holder.instant == null;
        }

        @Override
        public int hashCode() {
            return instant != null ? instant.hashCode() : 0;
        }
    }

    private Gson gson() {
        return new GsonBuilder().registerTypeAdapter(Instant.class, IsoFormatInstantTypeAdapter.getInstance()).create();
    }

}