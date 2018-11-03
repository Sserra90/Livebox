package com.creations.livebox.datasources.fetcher

import android.content.Context
import com.creations.livebox_common.serializers.Serializer
import com.creations.livebox_common.util.bufferedSource
import io.reactivex.Observable
import java.io.*
import java.lang.reflect.Type

/**
 * @author Sérgio Serra on 07/09/2018.
 * sergioserra99@gmail.com
 */
class FileFetcher<T> private constructor(
        private val mIs: InputStream,
        private val mType: Type,
        private val mSerializer: Serializer
) : Fetcher<T> {

    override fun fetch(): Observable<T> =
            Observable.fromCallable { mSerializer.deserialize<T>(bufferedSource(mIs), mType) }

    companion object {

        @JvmStatic
        fun <T> create(`is`: InputStream, type: Type, serializer: Serializer): FileFetcher<T> =
                FileFetcher(`is`, type, serializer)

        @JvmStatic
        @Throws(IOException::class)
        fun <T> create(context: Context, fileName: String, type: Type, serializer: Serializer): FileFetcher<T> =
                create(context.assets.open(fileName), type, serializer)

        @JvmStatic
        @Throws(FileNotFoundException::class)
        fun <T> create(file: File, type: Type, serializer: Serializer): FileFetcher<T> =
                create(FileInputStream(file), type, serializer)

        @JvmStatic
        @Throws(FileNotFoundException::class)
        fun <T> create(filename: String, type: Type, serializer: Serializer): FileFetcher<T> =
                create(FileInputStream(File(filename)), type, serializer)
    }
}
