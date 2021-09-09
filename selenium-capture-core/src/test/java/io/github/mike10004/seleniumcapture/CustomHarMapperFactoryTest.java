package io.github.mike10004.seleniumcapture;

import com.browserup.harreader.HarReaderMode;
import com.browserup.harreader.model.HarRequest;
import com.browserup.harreader.model.HarResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.*;

public class CustomHarMapperFactoryTest {


    @Test
    public void testHarMessageMixin_HarRequest() throws Exception {
        CustomHarMapperFactory cmf = new CustomHarMapperFactory();
        ObjectMapper mapper = cmf.instance(HarReaderMode.STRICT);
        HarRequest req = new HarRequest();
        checkState(req.getBodySize() == -1);
        String json = mapper.writeValueAsString(req);
        JsonObject deser = new Gson().fromJson(json, JsonObject.class);
        JsonElement bodySizeEl = deser.get("bodySize");
        assertTrue("body size", bodySizeEl == null || bodySizeEl.isJsonNull());
    }

    @Test
    public void testHarMessageMixin_HarResponse() throws Exception {
        CustomHarMapperFactory cmf = new CustomHarMapperFactory();
        ObjectMapper mapper = cmf.instance(HarReaderMode.STRICT);
        HarResponse rsp = new HarResponse();
        checkState(rsp.getBodySize() == -1);
        String json = mapper.writeValueAsString(rsp);
        JsonObject deser = new Gson().fromJson(json, JsonObject.class);
        JsonElement bodySizeEl = deser.get("bodySize");
        assertTrue("body size", bodySizeEl == null || bodySizeEl.isJsonNull());
    }

}