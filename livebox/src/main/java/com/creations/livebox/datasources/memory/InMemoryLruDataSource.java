package com.creations.livebox.datasources.memory;

import android.support.v4.util.LruCache;
import android.util.Log;

import com.creations.livebox.datasources.LocalDataSource;
import com.creations.livebox.util.Optional;

/**
 * @author Sérgio Serra on 28/08/2018.
 * <p>
 * An in-memory LRU data source.
 * <p>
 * sergioserra99@gmail.com
 */
public class InMemoryLruDataSource<I> implements LocalDataSource<I, I> {

    private static final String TAG = "InMemoryLruDataSource";

    private LruCache<String, I> mLruCache;

    private InMemoryLruDataSource() {
        int mCacheSize = 10 * 1024 * 1024;
        mLruCache = new LruCache<>(mCacheSize);
    }

    public static <I> LocalDataSource<I, I> create() {
        return new InMemoryLruDataSource<>();
    }

    @Override
    public Optional<I> read(String key) {
        Log.d(TAG, "Read from memory with key: " + key);
        return Optional.ofNullable(mLruCache.get(key));
    }

    @Override
    public void save(String key, I input) {
        Log.d(TAG, "Save to memory with key: " + key);
        mLruCache.put(key, input);
    }

    @Override
    public String toString() {
        return "InMemoryLruDataSource";
    }
}
