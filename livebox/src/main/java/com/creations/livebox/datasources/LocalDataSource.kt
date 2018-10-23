package com.creations.livebox.datasources

import java.lang.reflect.Type

/**
 * @author Sérgio Serra on 25/08/2018.
 * sergioserra99@gmail.com
 */
interface LocalDataSource<I, T> {
    val type: Type
    fun read(key: String): T?
    fun save(key: String, input: I)
    fun clear(key: String)
}
