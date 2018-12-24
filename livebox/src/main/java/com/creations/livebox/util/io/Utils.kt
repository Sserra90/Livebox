package com.creations.livebox.util.io

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.creations.livebox_common.util.bufferedSource
import java.io.Closeable
import java.io.File
import java.io.IOException
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Helper method to calculate the proper size limit of a cache instance.
 */
fun getCacheSizeInBytes(dir: File?, cacheSizePercent: Float, maxSizeInBytes: Long): Long {
    if (dir == null || !dir.exists() && !dir.mkdir()) {
        return 0
    }
    return try {
        val stat = StatFs(dir.path)
        val totalBytes = stat.blockCount * stat.blockSize.toLong()
        val freeBytes = stat.availableBlocks * stat.blockSize.toLong()
        val desiredBytes = Math.min(totalBytes * cacheSizePercent, maxSizeInBytes.toFloat()).toInt().toLong()
        // If free space is less than desired, use half of the free disk space instead.
        if (desiredBytes > freeBytes) freeBytes / 2 else desiredBytes
    } catch (e: IllegalArgumentException) {
        0
    }

}

/**
 * Helper method to initiate cache directory. It will return the cache directory in File format,
 * or NULL if the directory path is invalid or not accessible.
 */
fun getCacheDirectory(context: Context, path: String?): File? {
    var cacheDir: File? = null

    if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
        try {
            cacheDir = context.externalCacheDir
        } catch (e: NullPointerException) {
            // Fallback to use internal storage if external storage isn't available.
        }
    }

    if (cacheDir == null) {
        cacheDir = context.cacheDir
    }

    return if (cacheDir != null && path != null) File(cacheDir, path) else null
}

/**
 * Helper method to close a Closeable (e.g. InputStream/OutputStream) quietly without throwing any
 * additional IOExceptions.
 */
fun closeQuietly(closeable: Closeable?) {
    if (closeable != null) {
        try {
            closeable.close()
        } catch (e: IOException) {
            // Handle the IOException quietly.
        }

    }
}

/**
 * Reads file from assets folder as a String.
 * @param filename the path to the file relative to assets directory
 * @return file contents as a string
 */
inline fun <reified T : Any> bindAsset(filename: String): ReadOnlyProperty<T, String> =
        ResourcesVar(filename)

class ResourcesVar<T : Any>(private val filename: String) : ReadOnlyProperty<T, String> {
    override fun getValue(thisRef: T, property: KProperty<*>): String =
            bufferedSource(javaClass.classLoader!!.getResourceAsStream("assets/$filename")).readUtf8()
}