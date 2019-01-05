package com.sserra.livebox_jackson

import android.content.Context
import com.creations.convert_jackson.LiveboxJacksonSerializer
import com.creations.convert_jackson.util.fromRef
import com.creations.livebox.Box
import com.creations.livebox.config.Config
import com.creations.livebox.datasources.fetcher.Fetcher
import com.creations.livebox.datasources.fetcher.FileFetcher
import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Observable
import java.io.File
import java.io.InputStream

fun config(context: Context, objectMapper: ObjectMapper = ObjectMapper()) =
        Config.create(context, LiveboxJacksonSerializer.create(objectMapper))

inline fun <reified T, reified O> box(): Box<T, O> = Box(fromRef<T>())

inline fun <reified T> fileFetcher(context: Context, fileName: String) =
        FileFetcher.create<T>(context, fileName, fromRef<T>(), LiveboxJacksonSerializer.create())

inline fun <reified T> fileFetcher(fileName: String) =
        FileFetcher.create<T>(fileName, fromRef<T>(), LiveboxJacksonSerializer.create())

inline fun <reified T> fileFetcher(file: File) =
        FileFetcher.create<T>(file, fromRef<T>(), LiveboxJacksonSerializer.create())

inline fun <reified T> fileFetcher(iss: InputStream) =
        FileFetcher.create<T>(iss, fromRef<T>(), LiveboxJacksonSerializer.create())

fun <T> errorFetcher(throwable: Throwable = RuntimeException()): Fetcher<T> = object : Fetcher<T> {
    override fun fetch(): Observable<T> = Observable.error(throwable)
}

inline fun <reified T> Any.assetFetcher(fileName: String): Fetcher<T> =
        FileFetcher.create(
                javaClass.classLoader!!.getResourceAsStream("assets/$fileName"),
                fromRef<T>(),
                LiveboxJacksonSerializer.create()
        )