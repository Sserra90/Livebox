package com.creations.livebox.config

import android.content.Context
import com.creations.livebox.datasources.disk.DiskLruConfig
import com.creations.livebox.datasources.disk.DiskPersistentConfig
import com.creations.livebox.util.io.getCacheDirectory
import com.creations.livebox.util.io.getCacheSizeInBytes
import com.creations.livebox_common.serializers.Serializer
import java.io.File

data class Config(
        val diskLruConfig: DiskLruConfig, val persistentConfig: DiskPersistentConfig,
        val serializer: Serializer, val journalDir: File?,
        val disableCache: Boolean = false,
        private val disableLogging: Boolean
) {

    val isLoggingDisabled: Boolean
        get() = !disableLogging

    companion object {
        private const val LRU_DISK_CACHE_DIR = "livebox_disk_lru_cache"
        private const val PERSISTENT_DISK_CACHE_DIR = "livebox_disk_persistent_cache"
        private const val JOURNAL_DIR = "livebox_journal_dir"

        private const val DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 100 // 100MB
        private const val DEFAULT_DISK_CACHE_SIZE_PERCENT = 10 // 10% of free disk space

        @JvmStatic
        fun create(context: Context, serializer: Serializer): Config {

            val cacheDir = getCacheDirectory(context, LRU_DISK_CACHE_DIR)
            val lruCacheSize = getCacheSizeInBytes(
                    cacheDir,
                    DEFAULT_DISK_CACHE_SIZE_PERCENT / 100f,
                    DEFAULT_DISK_CACHE_SIZE.toLong()
            )
            val lruConfig = DiskLruConfig(cacheDir, lruCacheSize)
            val persistentConfig = DiskPersistentConfig(getCacheDirectory(context, PERSISTENT_DISK_CACHE_DIR))
            val journalDir = getCacheDirectory(context, JOURNAL_DIR)

            return Config(
                    diskLruConfig = lruConfig, persistentConfig = persistentConfig,
                    journalDir = journalDir, serializer = serializer,
                    disableCache = false, disableLogging = true
            )
        }
    }

    override fun toString(): String = "DiskLruConfig{" +
            "LruConfig=" + diskLruConfig +
            ", PersistentConfig=" + persistentConfig +
            ", Serializer=" + serializer +
            ", JournalDir=" + journalDir +
            '}'

}
