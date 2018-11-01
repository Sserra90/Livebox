package com.creations.livebox.datasources.disk

import com.creations.livebox.Livebox.Companion.TAG
import com.creations.livebox.datasources.LocalDataSource
import com.creations.livebox.util.io.closeQuietly
import com.creations.livebox_common.serializers.Serializer
import com.creations.livebox_common.util.Logger
import com.creations.livebox_common.util.bufferedSource
import com.instagram.igdiskcache.EditorOutputStream
import com.instagram.igdiskcache.IgDiskCache
import com.instagram.igdiskcache.OptionalStream
import com.instagram.igdiskcache.SnapshotInputStream
import okio.BufferedSource
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.Executors

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
class DiskLruDataSource<I, O> private constructor(
        private val mSerializer: Serializer,
        private val mType: Type
) : LocalDataSource<I, O> {

    companion object {
        lateinit var config: DiskLruConfig
        @JvmStatic
        fun <I, O> create(serializer: Serializer, type: Type): DiskLruDataSource<I, O> {
            LiveboxDiskCache.config = config
            return DiskLruDataSource(serializer, type)
        }
    }

    override val type: Type
        get() = mType

    override fun read(key: String): O? {
        val iis = LiveboxDiskCache[key]
        Logger.d(TAG, "Read from disk cache is present: %s with key: %s", iis.isPresent, key)
        var data: O? = null
        if (iis.isPresent) {
            data = mSerializer.deserialize<O>(bufferedSource(iis.get()), mType)
            Logger.d(TAG, "Data read from disk %s", data)
            return data
        }
        return data
    }

    override fun save(key: String, input: I) {
        val oos = LiveboxDiskCache.edit(key)
        Logger.d(TAG, "Save to disk cache is present: %s with key: %s", oos.isPresent, key)
        if (oos.isPresent) {
            try {
                mSerializer.serialize(input, mType)?.apply {
                    writeToCacheOutputStream(this, oos.get())
                    oos.get().commit()
                }
            } finally {
                oos.get().abortUnlessCommitted()
            }
        }
    }

    override fun clear(key: String) {
        Logger.d(TAG, "Clear key: %s", key)
        LiveboxDiskCache.clear(key)
    }

    private fun writeToCacheOutputStream(input: BufferedSource, output: EditorOutputStream) {
        val os = BufferedOutputStream(output)
        try {
            input.inputStream().copyTo(os)
            Logger.d(TAG, "---> Success data saved in diskLruDataSource.")
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            closeQuietly(input)
            closeQuietly(os)
        }
    }

    override fun toString() = "DiskLruDataSource"
}


private object LiveboxDiskCache {

    lateinit var config: DiskLruConfig

    private val diskCache: IgDiskCache by lazy {
        IgDiskCache(
                config.cacheDir,
                config.cacheSize,
                Executors.newSingleThreadExecutor()
        )
    }

    internal fun edit(key: String): OptionalStream<EditorOutputStream> = diskCache.edit(key)

    internal operator fun get(key: String): OptionalStream<SnapshotInputStream> = diskCache.get(key)

    internal fun clear(key: String) {
        diskCache.remove(key)
    }
}

class DiskLruConfig(val cacheDir: File?, cacheSize: Long) {
    val cacheSize: Long = Math.max(0, cacheSize)
    override fun toString(): String =
            "DiskLruConfig{" +
                    "CacheDir=" + cacheDir +
                    ", CacheSize=" + cacheSize +
                    '}'
}
