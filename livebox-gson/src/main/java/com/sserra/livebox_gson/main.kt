package com.sserra.livebox_gson

import android.content.Context
import com.creations.livebox.Box
import com.creations.livebox.config.Config
import com.creations.livebox.datasources.fetcher.Fetcher
import com.creations.livebox.datasources.fetcher.FileFetcher
import com.creations.serializer_gson.LiveboxGsonSerializer
import com.creations.serializer_gson.fromType
import com.google.gson.Gson
import io.reactivex.Observable
import java.io.File
import java.io.InputStream

fun config(context: Context, gson: Gson = Gson()) = Config.create(context, LiveboxGsonSerializer.create(gson))

inline fun <reified T, reified O> box(): Box<T, O> = Box(fromType<T>())

inline fun <reified T> fileFetcher(context: Context, fileName: String) =
        FileFetcher.create<T>(context, fileName, fromType<T>(), LiveboxGsonSerializer.create())

inline fun <reified T> fileFetcher(fileName: String) =
        FileFetcher.create<T>(fileName, fromType<T>(), LiveboxGsonSerializer.create())

inline fun <reified T> fileFetcher(file: File) =
        FileFetcher.create<T>(file, fromType<T>(), LiveboxGsonSerializer.create())


inline fun <reified T> fileFetcher(iss: InputStream) =
        FileFetcher.create<T>(iss, fromType<T>(), LiveboxGsonSerializer.create())


fun <T> errorFetcher(throwable: Throwable = RuntimeException()): Fetcher<T> =
        object : Fetcher<T> {
            override fun fetch(): Observable<T> = Observable.error(throwable)
        }

inline fun <reified T> Any.assetFetcher(fileName: String): Fetcher<T> =
        FileFetcher.create(
                javaClass.classLoader!!.getResourceAsStream("assets/$fileName"),
                fromType<T>(),
                LiveboxGsonSerializer.create()
        )