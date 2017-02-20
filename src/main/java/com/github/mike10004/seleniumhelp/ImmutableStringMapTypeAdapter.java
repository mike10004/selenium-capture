package com.github.mike10004.seleniumhelp;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Map;

class ImmutableStringMapTypeAdapter extends TypeAdapter<ImmutableMap<String, String>> {

    private final TypeAdapter<Map<String, String>> delegate;

    public ImmutableStringMapTypeAdapter() {
        delegate = new Gson().getAdapter(new TypeToken<Map<String, String>>(){});
    }

    @Override
    public void write(JsonWriter out, ImmutableMap<String, String> value) throws IOException {
        delegate.write(out, value);
    }

    @Override
    public ImmutableMap<String, String> read(JsonReader in) throws IOException {
        Map<String, String> map = delegate.read(in);
        return ImmutableMap.copyOf(map);
    }
}
