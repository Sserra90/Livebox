package com.creations.livebox.util;

import com.creations.livebox.datasources.LocalDataSource;

/**
 * @author Sérgio Serra on 09/09/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class FakeSource<I, T> implements LocalDataSource<I, T> {
    @Override
    public Optional<T> read(String key) {
        return null;
    }

    @Override
    public void save(String key, I input) {

    }

    @Override
    public void clear(String key) {

    }

}
