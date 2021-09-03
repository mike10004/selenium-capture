package io.github.mike10004.seleniumcapture;

import com.browserup.harreader.HarReaderMode;
import com.browserup.harreader.jackson.ExceptionIgnoringDateDeserializer;
import com.browserup.harreader.jackson.ExceptionIgnoringIntegerDeserializer;
import com.browserup.harreader.jackson.MapperFactory;
import com.browserup.harreader.model.HarRequest;
import com.browserup.harreader.model.HarResponse;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.Date;

@VisibleForTesting
class CustomHarMapperFactory implements MapperFactory {

    public CustomHarMapperFactory() {
        super();
    }

    @Override
    public ObjectMapper instance(HarReaderMode mode) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        if (mode == HarReaderMode.LAX) {
            module.addDeserializer(Date.class, new ExceptionIgnoringDateDeserializer());
            module.addDeserializer(Integer.class, new ExceptionIgnoringIntegerDeserializer());
        }
        module.setMixInAnnotation(HarRequest.class, HarMessageMixin.class);
        module.setMixInAnnotation(HarResponse.class, HarMessageMixin.class);
        mapper.registerModule(module);
        return mapper;
    }

    @SuppressWarnings("unused")
    private static abstract class HarMessageMixin {
        @JsonSerialize(using = DefaultSizeToNullSerializer.class)
        public Long headersSize;
        @JsonSerialize(using = DefaultSizeToNullSerializer.class)
        public Long bodySize;
    }

    /**
     * Serializer that replaces a value that means "size not set" with null.
     * The HarRequest/HarResponse classes' getter methods return -1 when the bodySize or
     * headersSize fields are null. Jackson serializes these objects using the values
     * from the getters instead of the real values. This serializer replaces a value
     * of -1 with null.
     */
    private static final class DefaultSizeToNullSerializer extends JsonSerializer<Long> {

        public DefaultSizeToNullSerializer() {
        }

        @Override
        public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == -1) {
                value = null;
            }
            serializers.defaultSerializeValue(value, gen);
        }
    }

}
