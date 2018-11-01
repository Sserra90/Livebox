package com.creations.livebox.datasources.factory

import com.creations.livebox.datasources.LocalDataSource

/**
 * @author Sérgio Serra on 26/08/2018.
 * sergioserra99@gmail.com
 */
interface DataSourceFactory<I> {
    operator fun <T> get(id: Int): LocalDataSource<I, T>?
}
