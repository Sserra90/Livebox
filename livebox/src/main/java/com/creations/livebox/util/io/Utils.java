package com.creations.livebox.util.io;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * @author Sérgio Serra on 25/08/2018.
 */
public class Utils {

    /**
     * Helper method to calculate the proper size limit of a cache instance.
     */
    public static long getCacheSizeInBytes(File dir, float cacheSizePercent, long maxSizeInBytes) {
        if (dir == null || (!dir.exists() && !dir.mkdir())) {
            return 0;
        }
        try {
            StatFs stat = new StatFs(dir.getPath());
            int totalBytes = stat.getBlockCount() * stat.getBlockSize();
            int freeBytes = stat.getAvailableBlocks() * stat.getBlockSize();
            int desiredBytes = (int) Math.min((totalBytes * cacheSizePercent), maxSizeInBytes);
            // If free space is less than desired, use half of the free disk space instead.
            desiredBytes = (desiredBytes > freeBytes) ? freeBytes / 2 : desiredBytes;
            return desiredBytes;
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    /**
     * Helper method to initiate cache directory. It will return the cache directory in File format,
     * or NULL if the directory path is invalid or not accessible.
     */
    public static File getCacheDirectory(final Context context, final String path) {
        File cacheDir = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                cacheDir = context.getExternalCacheDir();
            } catch (NullPointerException e) {
                // Fallback to use internal storage if external storage isn't available.
            }
        }
        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        }
        return (cacheDir != null && path != null) ? new File(cacheDir, path) : null;
    }

    /**
     * Helper method to close a Closeable (e.g. InputStream/OutputStream) quietly without throwing any
     * additional IOExceptions.
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Handle the IOException quietly.
            }
        }
    }
}
