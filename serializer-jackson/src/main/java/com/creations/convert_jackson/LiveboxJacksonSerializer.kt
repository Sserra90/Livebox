package com.creations.convert_jackson

import com.creations.livebox_common.serializers.Serializer
import com.creations.livebox_common.util.Logger
import com.creations.livebox_common.util.bufferedSource
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import okio.BufferedSource
import java.io.ByteArrayInputStream
import java.io.IOException
import java.lang.reflect.Type

/**
 * @author Sérgio Serra
 * sergioserra99@gmail.com
 */
class LiveboxJacksonSerializer private constructor(private val mObjectMapper: ObjectMapper) : Serializer {

    override fun <T> deserialize(source: BufferedSource, type: Type): T? {
         try {
            if (Class::class.java.isAssignableFrom(type.javaClass)) {
                Logger.d(TAG, "Deserialize for class: $type")

                @Suppress("UNCHECKED_CAST")
                return mObjectMapper.readValue(source.inputStream(), type as Class<T>)
            } else if (JavaType::class.java.isAssignableFrom(type.javaClass)) {
                Logger.d(TAG, "Deserialize for type: $type")
                return mObjectMapper.readValue<T>(source.inputStream(), type as JavaType)
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    override fun <T> serialize(input: T, type: Type): BufferedSource? {
        try {
            Logger.d(TAG, "Serialize for type: $type")
            mObjectMapper.writeValueAsBytes(input)?.apply {
                if (isNotEmpty()) {
                    return bufferedSource(ByteArrayInputStream(this))
                }
            }
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
        }

        return null
    }

    companion object {
        private const val TAG = "LiveboxJacksonSerialize"
        @JvmStatic
        fun create(objectMapper: ObjectMapper): Serializer = LiveboxJacksonSerializer(objectMapper)
    }

}
