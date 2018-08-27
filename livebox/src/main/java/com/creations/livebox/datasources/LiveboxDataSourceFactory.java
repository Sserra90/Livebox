package com.creations.livebox.datasources;

import com.creations.livebox.util.Optional;
import com.creations.livebox.validator.DataValidator;

/**
 * @author Sérgio Serra on 26/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public final class LiveboxDataSourceFactory<I, O> implements DataSourceFactory<I> {

    private String mCacheKey;
    private Class<?> mTargetClass;
    private DataValidator<O> mDataValidator;

    public LiveboxDataSourceFactory() {
    }

    public LiveboxDataSourceFactory<I, O> setCacheKey(String cacheKey) {
        this.mCacheKey = cacheKey;
        return this;
    }

    public LiveboxDataSourceFactory<I, O> setTargetClass(Class<?> targetClass) {
        this.mTargetClass = targetClass;
        return this;
    }

    public LiveboxDataSourceFactory<I, O> setDataValidator(DataValidator<O> dataValidator) {
        this.mDataValidator = dataValidator;
        return this;
    }

    @Override
    public Optional<LocalDataSource<I, ?>> get(int id) {
        LocalDataSource<I, ?> dataSource = null;
        switch (id) {
            case Sources.DISK_LRU:
                dataSource = DiskLruDataSource.create(mCacheKey, mTargetClass, Optional.ofNullable(mDataValidator));
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
