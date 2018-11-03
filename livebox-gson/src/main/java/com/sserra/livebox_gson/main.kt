package com.sserra.livebox_gson

import android.content.Context
import com.creations.livebox.Box
import com.creations.livebox.config.Config
import com.creations.livebox.datasources.fetcher.FileFetcher
import com.creations.serializer_gson.LiveboxGsonSerializer
import com.creations.serializer_gson.fromType
import java.io.File
import java.io.InputStream

fun config(context: Context) = Config.create(context, LiveboxGsonSerializer.create())

inline fun <reified T, reified O> box(): Box<T, O> = Box(fromType<T>())

inline fun <reified T> fileFetcher(context: Context, fileName: String) =
        FileFetcher.create<T>(context, fileName, fromType<T>(), LiveboxGsonSerializer.create())

inline fun <reified T> fileFetcher(fileName: String) =
        FileFetcher.create<T>(fileName, fromType<T>(), LiveboxGsonSerializer.create())

inline fun <reified T> fileFetcher(file: File) =
        FileFetcher.create<T>(file, fromType<T>(), LiveboxGsonSerializer.create())


inline fun <reified T> fileFetcher(iss: InputStream) =
        FileFetcher.create<T>(iss, fromType<T>(), LiveboxGsonSerializer.create())