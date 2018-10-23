package com.sserra.livebox_jackson

import android.content.Context
import com.creations.convert_jackson.LiveboxJacksonSerializer
import com.creations.convert_jackson.util.fromRef
import com.creations.livebox.Box
import com.creations.livebox.config.Config
import com.creations.livebox.datasources.fetcher.FileFetcher
import java.io.File
import java.io.InputStream

fun config(context: Context) = Config.create(context, LiveboxJacksonSerializer.create())

inline fun <reified T, reified O> box(): Box<T, O> = Box(fromRef<T>())

inline fun <reified T> fileFetcher(context: Context, fileName: String) =
        FileFetcher.create<T>(context, fileName, fromRef<T>(), LiveboxJacksonSerializer.create())

inline fun <reified T> fileFetcher(fileName: String) =
        FileFetcher.create<T>(fileName, fromRef<T>(), LiveboxJacksonSerializer.create())

inline fun <reified T> fileFetcher(file: File) =
        FileFetcher.create<T>(file, fromRef<T>(), LiveboxJacksonSerializer.create())

inline fun <reified T> fileFetcher(iss: InputStream) =
        FileFetcher.create<T>(iss, fromRef<T>(), LiveboxJacksonSerializer.create())