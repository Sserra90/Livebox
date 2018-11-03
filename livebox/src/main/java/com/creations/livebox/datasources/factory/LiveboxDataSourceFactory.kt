package com.creations.livebox.datasources.factory

import com.creations.livebox.datasources.LocalDataSource
import com.creations.livebox.datasources.disk.DiskLruDataSource
import com.creations.livebox.datasources.disk.DiskPersistentDataSource
import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory.Sources.DISK_LRU
import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory.Sources.DISK_PERSISTENT
import com.creations.livebox_common.serializers.Serializer
import java.lang.reflect.Type

/**
 * @author Sérgio Serra on 26/08/2018.
 * sergioserra99@gmail.com
 */
class LiveboxDataSourceFactory<I>(private val mSerializer: Serializer, private val mType: Type) : DataSourceFactory<I> {

    override fun <T> get(id: Int): LocalDataSource<I, T>? {
        var dataSource: LocalDataSource<I, T>? = null
        when (id) {
            DISK_LRU -> dataSource = DiskLruDataSource.create(mSerializer, mType)
            DISK_PERSISTENT -> dataSource = DiskPersistentDataSource.create(mSerializer, mType)
        }
        return dataSource
    }

    object Sources {
        const val DISK_LRU = 2000
        const val DISK_PERSISTENT = 3000
    }
}
