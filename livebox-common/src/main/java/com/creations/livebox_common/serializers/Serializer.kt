package com.creations.livebox_common.serializers

import okio.BufferedSource
import java.lang.reflect.Type

/**
 * @author Sérgio Serra
 * sergioserra99@gmail.com
 */
interface Serializer {
    fun <T> serialize(input: T, type: Type): BufferedSource?

    fun <T> deserialize(source: BufferedSource, type: Type): T?
}
