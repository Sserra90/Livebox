package com.creations.app.repo

import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory.Sources
import com.sserra.livebox_jackson.box
import io.reactivex.Observable
import java.util.*
import java.util.concurrent.TimeUnit

class UsersRepo {

    val users: Observable<List<Int>>
        get() = box<List<String>, List<Int>>()
                .withKey("users")
                .fetch { Observable.just(listOf("1")).delay(5, TimeUnit.SECONDS) }
                .addSource(Sources.DISK_LRU) { _, _ -> true }
                .ignoreCache(true)
                .addConverter<List<Int>> { ArrayList() }
                .build()
                .asAndroidObservable()
}
