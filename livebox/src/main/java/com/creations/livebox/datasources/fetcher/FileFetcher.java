package com.creations.livebox.datasources.fetcher;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import io.reactivex.Observable;

/**
 * @author Sérgio Serra on 07/09/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class FileFetcher<T> implements Fetcher<T> {

    private InputStream mIs;
    private Gson mGson;
    private Type mType;

    public static <T> FileFetcher<T> create(InputStream is, Type type) {
        return new FileFetcher<>(is, type);
    }

    public static <T> FileFetcher<T> create(Context context, String fileName, Type type) throws IOException {
        return create(context.getAssets().open(fileName), type);
    }

    public static <T> FileFetcher<T> create(File file, Type type) throws FileNotFoundException {
        return create(new FileInputStream(file), type);
    }

    private FileFetcher(InputStream is, Type type) {
        mIs = is;
        mGson = new Gson();
        mType = type;
    }

    @Override
    public Observable<T> fetch() {
        return Observable.fromCallable(() -> mGson.fromJson(new JsonReader(new InputStreamReader(mIs)), mType));
    }
}
