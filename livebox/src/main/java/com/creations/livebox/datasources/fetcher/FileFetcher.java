package com.creations.livebox.datasources.fetcher;

import android.content.Context;

import com.creations.livebox_common.serializers.Serializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

import io.reactivex.Observable;

import static com.creations.livebox_common.util.OkioUtils.bufferedSource;

/**
 * @author Sérgio Serra on 07/09/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class FileFetcher<T> implements Fetcher<T> {

    private InputStream mIs;
    private Serializer mSerializer;
    private Type mType;

    public static <T> FileFetcher<T> create(InputStream is, Type type, Serializer serializer) {
        return new FileFetcher<>(is, type, serializer);
    }

    public static <T> FileFetcher<T> create(Context context, String fileName, Type type, Serializer serializer) throws IOException {
        return create(context.getAssets().open(fileName), type, serializer);
    }

    public static <T> FileFetcher<T> create(File file, Type type, Serializer serializer) throws FileNotFoundException {
        return create(new FileInputStream(file), type, serializer);
    }

    public static <T> FileFetcher<T> create(String filename, Type type, Serializer serializer) throws FileNotFoundException {
        return create(new FileInputStream(new File(filename)), type, serializer);
    }

    private FileFetcher(InputStream is, Type type, Serializer serializer) {
        mIs = is;
        mSerializer = serializer;
        mType = type;
    }

    @Override
    public Observable<T> fetch() {
        return Observable.fromCallable(() -> mSerializer.deserialize(bufferedSource(mIs), mType));
    }
}
