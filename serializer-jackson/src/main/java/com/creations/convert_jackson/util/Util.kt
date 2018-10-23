package com.creations.convert_jackson.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.type.TypeFactory

import java.lang.reflect.Type

/**
 * @author Sérgio Serra
 * sergioserra99@gmail.com
 */
fun fromClass(aClass: Class<*>): Type = TypeFactory.defaultInstance().constructType(aClass)

inline fun <reified T> fromRef(): Type = TypeFactory.defaultInstance().constructType(object : TypeRef<T>() {})
fun <T> fromRef(ref:TypeReference<T>): Type = TypeFactory.defaultInstance().constructType(ref)

open class TypeRef<T> : TypeReference<T>()