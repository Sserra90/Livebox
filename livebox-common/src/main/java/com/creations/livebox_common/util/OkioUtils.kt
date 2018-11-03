package com.creations.livebox_common.util

import okio.BufferedSource
import okio.Okio
import java.io.InputStream
import java.io.StringReader
import java.nio.charset.Charset

/**
 * Represents the end-of-file (or stream).
 */
const val EOF = -1

fun readerInputStreamUtf8(source: String): ReaderInputStream =
        ReaderInputStream(StringReader(source), Charset.forName("UTF-8"))

fun bufferedSource(`is`: InputStream): BufferedSource = Okio.buffer(Okio.source(`is`))
