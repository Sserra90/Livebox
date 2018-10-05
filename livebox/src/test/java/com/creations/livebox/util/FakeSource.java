package com.creations.livebox.util;

import com.creations.livebox.datasources.LocalDataSource;

import java.lang.reflect.Type;

/***
 * Fake source used on testing
 * @author Sérgio Serra
 */
public class FakeSource<I, T> implements LocalDataSource<I, T> {

    public static <I, T> FakeSource<I, T> create() {
        return new FakeSource<>();
    }

    private I data;

    @Override
    public Optional<T> read(String key) {
        //noinspection unchecked
        return Optional.ofNullable((T) data);
    }

    @Override
    public void save(String key, I input) {
        data = input;
    }

    @Override
    public void clear(String key) {

    }

    @Override
    public Type getType() {
        return null;
    }
}
