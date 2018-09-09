package com.creations.livebox.serializers;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.InputStreamReader;
import java.lang.reflect.Type;

import okio.BufferedSource;

import static com.creations.livebox.util.io.OkioUtils.bufferedSource;
import static com.creations.livebox.util.io.OkioUtils.readerInputStreamUtf8;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class LiveboxGsonSerializer<T> implements Serializer<T> {

    private Type mType;
    private Gson gson;

    private LiveboxGsonSerializer(Type type) {
        mType = type;
        gson = new Gson();
    }

    public static <T> LiveboxGsonSerializer<T> create(TypeToken mType) {
        return new LiveboxGsonSerializer<>(mType.getType());
    }

    public static <T> LiveboxGsonSerializer<T> create(Type mType) {
        return new LiveboxGsonSerializer<>(mType);
    }

    @Override
    public BufferedSource serialize(T input) {
        return bufferedSource(readerInputStreamUtf8(gson.toJson(input, mType)));
    }

    @Override
    public T deserialize(BufferedSource source) {
        if (source == null) {
            return null;
        }

        try {
            return gson.fromJson(new JsonReader(new InputStreamReader(source.inputStream())), mType);
        } catch (JsonIOException e) {
            e.printStackTrace();
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }

        return null;
    }
}
