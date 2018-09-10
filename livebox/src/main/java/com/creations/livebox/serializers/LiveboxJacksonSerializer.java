package com.creations.livebox.serializers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;

import okio.BufferedSource;

import static com.creations.livebox.util.io.OkioUtils.bufferedSource;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class LiveboxJacksonSerializer implements Serializer {

    private ObjectMapper mObjectMapper;

    private LiveboxJacksonSerializer(ObjectMapper objectMapper) {
        mObjectMapper = objectMapper;
    }

    public static LiveboxJacksonSerializer create() {
        return new LiveboxJacksonSerializer(new ObjectMapper());
    }

    public static LiveboxJacksonSerializer create(ObjectMapper objectMapper) {
        return new LiveboxJacksonSerializer(objectMapper);
    }

    @Override
    public <T> T deserialize(BufferedSource source, Type type) {
        if (source == null) {
            return null;
        }

        if (JavaType.class.isAssignableFrom(type.getClass())) {
            try {
                return mObjectMapper.readValue(source.inputStream(), (JavaType) type);
            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public <T> BufferedSource serialize(T input, Type type) {
        try {
            return bufferedSource(new ByteArrayInputStream(mObjectMapper.writeValueAsBytes(input)));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

}
