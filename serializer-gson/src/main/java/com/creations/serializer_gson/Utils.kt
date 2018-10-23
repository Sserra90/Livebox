package com.creations.serializer_gson

import com.google.gson.reflect.TypeToken

import java.lang.reflect.Type

/**
 * @author Sérgio Serra on 11/09/2018.
 * Criations
 * sergioserra99@gmail.com
 */
inline fun <reified T> fromType(): Type = object : TypeToken<T>() {}.type

fun <T> fromType(token: TypeToken<T>): Type = token.type
