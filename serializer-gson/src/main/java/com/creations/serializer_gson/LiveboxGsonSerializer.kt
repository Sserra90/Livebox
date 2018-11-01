package com.creations.serializer_gson

import com.creations.livebox_common.serializers.Serializer
import com.creations.livebox_common.util.bufferedSource
import com.creations.livebox_common.util.readerInputStreamUtf8
import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonReader
import okio.BufferedSource
import java.io.InputStreamReader
import java.lang.reflect.Type


/**
 * @author Sérgio Serra
 * sergioserra99@gmail.com
 */
class LiveboxGsonSerializer private constructor(private val mGson: Gson) : Serializer {

    override fun <T> serialize(input: T, type: Type): BufferedSource? {
        try {
            return bufferedSource(readerInputStreamUtf8(mGson.toJson(input, type)))
        } catch (e: JsonIOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun <T> deserialize(source: BufferedSource, type: Type): T? {
        try {
            return mGson.fromJson<T>(JsonReader(InputStreamReader(source.inputStream())), type)
        } catch (e: JsonIOException) {
            e.printStackTrace()
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
        }
        return null
    }

    companion object {
        @JvmStatic
        fun create(gson: Gson = Gson()): Serializer = LiveboxGsonSerializer(gson)

        @JvmStatic
        fun create(): Serializer = create(Gson())
    }
}
