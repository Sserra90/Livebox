package com.creations.livebox.datasources.disk

import com.creations.livebox.Livebox.Companion.TAG
import com.creations.livebox.datasources.LocalDataSource
import com.creations.livebox_common.serializers.Serializer
import com.creations.livebox_common.util.Logger
import okio.BufferedSource
import okio.Okio
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.reflect.Type

/**
 * @author Sérgio Serra on 25/08/2018.
 * sergioserra99@gmail.com
 */
class DiskPersistentDataSource<I, O> private constructor(
        private val mSerializer: Serializer,
        override val type: Type
) : LocalDataSource<I, O> {

    companion object {
        private const val SUFFIX = "_livebox.json"
        lateinit var config: DiskPersistentConfig

        @JvmStatic
        fun <I, O> create(serializer: Serializer, type: Type): DiskPersistentDataSource<I, O> =
                DiskPersistentDataSource(serializer, type)
    }

    override fun read(key: String): O? {
        Logger.d(TAG, "Read from disk with  key: $key")
        return readFromDisk(key)
    }

    @Throws(IllegalStateException::class)
    override fun save(key: String, input: I) {
        Logger.d(TAG, "Save to disk with  key: $key")
        writeToDisk(key, mSerializer.serialize(input, type))
    }

    override fun clear(key: String) {
        val outputFile = File(config.outputDir, key + SUFFIX)
        if (outputFile.exists()) {
            Logger.d(TAG, "Delete file: " + outputFile.name)
            outputFile.delete()
        }
    }

    private fun readFromDisk(fileName: String): O? {

        if (config.outputDir == null) {
            return null
        }

        val outputFile = File(config.outputDir, fileName + SUFFIX)
        if (!outputFile.canRead()) {
            return null
        }

        Logger.d(TAG, "File available, read it")

        var data: O? = null
        try {
            Okio.buffer(Okio.source(outputFile)).use {
                data = mSerializer.deserialize<O>(it, type)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        return data
    }

    private fun writeToDisk(fileName: String, input: BufferedSource?) {

        if (config.outputDir == null || input == null) {
            return
        }

        var created = true
        if (!config.outputDir!!.exists()) {
            created = config.outputDir!!.mkdir()
        }

        // Cannot create file
        if (!created) {
            Logger.e(TAG, "Cannot create DiskPersistentDataSource output dir")
            return
        }

        val outputFile = File(config.outputDir, fileName + SUFFIX)
        try {
            input.inputStream().use {
                Okio.buffer(Okio.sink(outputFile)).outputStream().use { os ->
                    it.copyTo(os)
                    Logger.d(TAG, "Success data saved in diskPersistentDataSource.")
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun toString() = "DiskPersistentDataSource"
}

data class DiskPersistentConfig(internal val outputDir: File?)
