package com.creations.livebox.datasources;

import com.creations.livebox.Logger;
import com.creations.livebox.serializers.LiveboxGsonSerializer;
import com.creations.livebox.serializers.Serializer;
import com.instagram.igdiskcache.EditorOutputStream;
import com.instagram.igdiskcache.IgDiskCache;
import com.instagram.igdiskcache.OptionalStream;
import com.instagram.igdiskcache.SnapshotInputStream;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.Executors;

import okio.BufferedSource;

import static com.creations.livebox.util.OkioUtils.bufferedSource;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class DiskLruDataSource<I, O> implements LocalDataSource<I, O> {

    private static final String TAG = "DiskLruDataSource";
    private static DiskLruDataSource.Config mDiskCacheConfig;
    private String mKey;
    private LiveboxDiskCache mDiskCache;
    private Serializer<I> mSerializer;

    private DiskLruDataSource(String key, Type type) {
        mKey = key;
        mDiskCache = LiveboxDiskCache.getInstance(mDiskCacheConfig);
        mSerializer = LiveboxGsonSerializer.create(type);
    }

    public static <I, O> DiskLruDataSource<I, O> create(String key, Type type) {
        return new DiskLruDataSource<>(key, type);
    }

    public static void setConfig(Config config) {
        DiskLruDataSource.mDiskCacheConfig = config;
    }

    @Override
    public O read() {
        OptionalStream<SnapshotInputStream> iis = mDiskCache.get(mKey);
        Logger.d(TAG, "Read from disk cache is present: " + iis.isPresent() + " with  key: " + mKey);
        if (iis.isPresent()) {
            //noinspection unchecked
            return (O) mSerializer.deserialize(bufferedSource(iis.get()));
        }
        return null;
    }

    @Override
    public void save(I input) throws IllegalStateException {
        OptionalStream<EditorOutputStream> oos = mDiskCache.edit(mKey);
        Logger.d(TAG, "Save to disk cache is present: " + oos.isPresent() + " with  key: " + mKey);
        if (oos.isPresent()) {
            try {
                writeToCacheOutputStream(mSerializer.serialize(input), oos.get());
                oos.get().commit();
            } finally {
                oos.get().abortUnlessCommitted();
            }
        }
    }

    private void writeToCacheOutputStream(BufferedSource input, EditorOutputStream output) {
        try {
            IOUtils.copy(input.inputStream(), output);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Logger.d(TAG, "Success -> Wrote to disk cache output stream.");
    }

    public static class Config {
        private File mCacheDir;
        private long mCacheSize;

        public Config(File mCacheDir, long mCacheSize) {
            this.mCacheDir = mCacheDir;
            this.mCacheSize = mCacheSize;
        }

        File getCacheDir() {
            return mCacheDir;
        }

        long getCacheSize() {
            return mCacheSize;
        }
    }

    private static class LiveboxDiskCache {

        private static LiveboxDiskCache instance;

        private Config mConfig;
        private IgDiskCache mDiskCache;

        private LiveboxDiskCache(Config config) {
            mConfig = config;
            //mDiskCache = getDiskCache();
        }

        static LiveboxDiskCache getInstance(Config config) {
            if (instance == null) {
                synchronized (LiveboxDiskCache.class) {
                    if (instance == null) {
                        instance = new LiveboxDiskCache(config);
                    }
                }
            }
            return instance;
        }

        OptionalStream<EditorOutputStream> edit(String key) {
            return getDiskCache().edit(key);
        }

        OptionalStream<SnapshotInputStream> get(String key) {
            return getDiskCache().get(key);
        }

        private IgDiskCache getDiskCache() {
            // lazy initialization of IgDiskCache to avoid calling it from the main UI thread.
            if (mDiskCache == null) {
                mDiskCache = new IgDiskCache(
                        mConfig.getCacheDir(),
                        mConfig.getCacheSize(),
                        Executors.newSingleThreadExecutor()
                );
            }
            return mDiskCache;
        }
    }

}
