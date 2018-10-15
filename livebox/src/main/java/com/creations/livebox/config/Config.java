package com.creations.livebox.config;


import android.content.Context;

import com.creations.livebox.datasources.disk.DiskLruDataSource;
import com.creations.livebox.datasources.disk.DiskPersistentDataSource;
import com.creations.livebox.util.io.Utils;
import com.creations.livebox_common.serializers.Serializer;

import java.io.File;

import io.reactivex.internal.functions.ObjectHelper;

public final class Config {

    private static final String LRU_DISK_CACHE_DIR = "livebox_disk_lru_cache";
    private static final String PERSISTENT_DISK_CACHE_DIR = "livebox_disk_persistent_cache";
    private static final String JOURNAL_DIR = "livebox_journal_dir";

    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 100; // 100MB
    private static final int DEFAULT_DISK_CACHE_SIZE_PERCENT = 10; // 10% of free disk space


    private DiskLruDataSource.Config mLruConfig;
    private DiskPersistentDataSource.Config mPersistentConfig;
    private Serializer mSerializer;
    private File mJournalDir;
    private boolean mDisableLogging;

    public Config() {
    }

    public Config(Context context) {
        final File lruCacheDir = Utils.getCacheDirectory(context, LRU_DISK_CACHE_DIR);
        final long lurCacheSize = Utils.getCacheSizeInBytes(
                lruCacheDir,
                DEFAULT_DISK_CACHE_SIZE_PERCENT / 100F,
                DEFAULT_DISK_CACHE_SIZE
        );
        final File persistentCacheDir = Utils.getCacheDirectory(context, PERSISTENT_DISK_CACHE_DIR);
        journalDir(Utils.getCacheDirectory(context, JOURNAL_DIR));
        lruCacheConfig(new DiskLruDataSource.Config(lruCacheDir, lurCacheSize));
        persistentCacheConfig(new DiskPersistentDataSource.Config(persistentCacheDir));
    }

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

    public Config log(boolean val) {
        mDisableLogging = val;
        return this;
    }

    public File getJournalDir() {
        return mJournalDir;
    }

    public DiskLruDataSource.Config getLruConfig() {
        return mLruConfig;
    }

    public boolean isLoggingDisabled() {
        return mDisableLogging;
    }

    public DiskPersistentDataSource.Config getPersistentConfig() {
        return mPersistentConfig;
    }

    public Serializer getSerializer() {
        return mSerializer;
    }

    @Override
    public String toString() {
        return "Config{" +
                "LruConfig=" + mLruConfig +
                ", PersistentConfig=" + mPersistentConfig +
                ", Serializer=" + mSerializer +
                ", JournalDir=" + mJournalDir +
                '}';
    }
}
