package com.creations.livebox.util

import com.creations.livebox.datasources.LocalDataSource

import java.lang.reflect.Type

/***
 * Fake source used on testing.
 * @author Sérgio Serra
 */
open class FakeSource<I, T> : LocalDataSource<I, T> {

    private var data: I? = null

    override val type: Type = Any::class.java

    override fun read(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return data as T?
    }

    override fun save(key: String, input: I) {
        data = input
    }

    override fun clear(key: String) {}

    companion object {
        @JvmStatic
        fun <I, T> create(): FakeSource<I, T> = FakeSource()
    }
}
