package com.creations.livebox.serializers;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
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
public class LiveboxGsonSerializer implements Serializer {

    private Gson mGson;

    private LiveboxGsonSerializer(Gson gson) {
        mGson = gson;
    }

    public static LiveboxGsonSerializer create() {
        return new LiveboxGsonSerializer(new Gson());
    }

    public static LiveboxGsonSerializer create(Gson gson) {
        return new LiveboxGsonSerializer(gson);
    }

    @Override
    public <T> BufferedSource serialize(T input, Type type) {
        return bufferedSource(readerInputStreamUtf8(mGson.toJson(input, type)));
    }

    @Override
    public <T> T deserialize(BufferedSource source, Type type) {
        if (source == null) {
            return null;
        }

        try {
            return mGson.fromJson(new JsonReader(new InputStreamReader(source.inputStream())), type);
        } catch (JsonIOException e) {
            e.printStackTrace();
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }

        return null;
    }
}
