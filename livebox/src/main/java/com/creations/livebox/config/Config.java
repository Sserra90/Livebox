package com.creations.livebox.config;


import com.creations.livebox.datasources.disk.DiskLruDataSource;
import com.creations.livebox.datasources.disk.DiskPersistentDataSource;
import com.creations.livebox_common.serializers.Serializer;

import java.io.File;

import io.reactivex.internal.functions.ObjectHelper;

public final class Config {

    private DiskLruDataSource.Config mLruConfig;
    private DiskPersistentDataSource.Config mPersistentConfig;
    private Serializer mSerializer;
    private File mJournalDir;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public Config lruCacheConfig(DiskLruDataSource.Config diskLruConfig) {
        ObjectHelper.requireNonNull(diskLruConfig, "Lru disk cache config cannot be null");
        mLruConfig = diskLruConfig;
        return this;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public Config persistentCacheConfig(DiskPersistentDataSource.Config diskCacheConfig) {
        ObjectHelper.requireNonNull(diskCacheConfig, "Persistent disk cache config cannot be null");
        mPersistentConfig = diskCacheConfig;
        return this;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public Config journalDir(File journalDir) {
        ObjectHelper.requireNonNull(journalDir, "Journal dir cannot be null");
        mJournalDir = journalDir;
        return this;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public Config addSerializer(Serializer serializer) {
        ObjectHelper.requireNonNull(serializer, "Serializer instance cannot be null");
        mSerializer = serializer;
        return this;
    }

    public File getJournalDir() {
        return mJournalDir;
    }

    public DiskLruDataSource.Config getLruConfig() {
        return mLruConfig;
    }

    public DiskPersistentDataSource.Config getPersistentConfig() {
        return mPersistentConfig;
    }

    public Serializer getSerializer() {
        return mSerializer;
    }

}
