package com.creations.livebox.datasources;

import com.creations.livebox.util.Optional;

import java.lang.reflect.Type;

/**
 * @author Sérgio Serra on 26/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public final class LiveboxDataSourceFactory<I> implements DataSourceFactory<I> {

    private String mCacheKey;
    private Type mTargetType;

    public LiveboxDataSourceFactory<I> setCacheKey(String cacheKey) {
        this.mCacheKey = cacheKey;
        return this;
    }

    public LiveboxDataSourceFactory<I> setType(Type type) {
        this.mTargetType = type;
        return this;
    }

    @Override
    public <T> Optional<LocalDataSource<I, T>> get(int id) {
        LocalDataSource<I, T> dataSource = null;
        switch (id) {
            case Sources.DISK_LRU:
                dataSource = DiskLruDataSource.create(mCacheKey, mTargetType);
                break;
        }
        return Optional.ofNullable(dataSource);
    }

    public static abstract class Sources {
        public static final int MEMORY_LRU = 1000;
        public static final int DISK_LRU = 2000;
        public static final int DISK_PERMANENT = 3000;
    }

}
