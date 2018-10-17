package com.creations.convert_jackson;

import com.creations.livebox_common.serializers.Serializer;
import com.creations.livebox_common.util.Logger;
import com.creations.livebox_common.util.OkioUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;

import okio.BufferedSource;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class LiveboxJacksonSerializer implements Serializer {

    private static final String TAG = "LiveboxJacksonSerialize";

    private ObjectMapper mObjectMapper;

    private LiveboxJacksonSerializer(ObjectMapper objectMapper) {
        mObjectMapper = objectMapper;
    }

    public static Serializer create() {
        return new LiveboxJacksonSerializer(new ObjectMapper());
    }

    public static Serializer create(ObjectMapper objectMapper) {
        return new LiveboxJacksonSerializer(objectMapper);
    }

    @Override
    public <T> T deserialize(BufferedSource source, Type type) {
        if (source == null) {
            return null;
        }

        try {
            if (Class.class.isAssignableFrom(type.getClass())) {
                Logger.d(TAG, "Deserialize for class: " + type);
                //noinspection unchecked
                return mObjectMapper.readValue(source.inputStream(), (Class<T>) type);
            } else if (JavaType.class.isAssignableFrom(type.getClass())) {
                Logger.d(TAG, "Deserialize for type: " + type);
                return mObjectMapper.readValue(source.inputStream(), (JavaType) type);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public <T> BufferedSource serialize(T input, Type type) {
        try {
            Logger.d(TAG, "Serialize for type: " + type);
            byte[] bytes = mObjectMapper.writeValueAsBytes(input);
            if (bytes != null && bytes.length > 0) {
                return OkioUtils.bufferedSource(new ByteArrayInputStream(bytes));
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

}
