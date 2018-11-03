package com.creations.app.repo

import com.creations.app.api.Api
import com.creations.app.api.GithubService
import com.creations.app.api.UsersRes
import com.creations.app.entities.Users
import com.creations.app.room.Db
import com.creations.app.room.UsersDao
import com.creations.app.room.mapToEntities
import com.creations.app.room.mapToUsers
import com.creations.livebox.datasources.LocalDataSource
import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory.Sources
import com.creations.livebox.validator.minutes
import com.creations.livebox_common.util.Logger
import com.sserra.livebox_jackson.box
import io.reactivex.Observable
import java.lang.reflect.Type

class UsersRepo(private val api: GithubService = Api.getInstance().githubService) {

    val users: Observable<Users>
        get() = box<UsersRes, Users>()
                .withKey("users")
                .fetch { api.userList }
                .addSource<UsersRes>(Sources.DISK_LRU, 1.minutes())
                .addSource(UsersRoomDataSource()) { _, users -> users.items.isNotEmpty() }
                .addConverter<UsersRes> { Users.fromUsersRes(it) }
                .retryOnFailure()
                .build()
                .asAndroidObservable()
}

class UsersRoomDataSource(private val usersDao: UsersDao = Db.usersDao()) : LocalDataSource<UsersRes, Users> {
    companion object {
        const val TAG = "UsersRoomDataSource"
    }

    override val type: Type
        get() = Users::class.java

    override fun read(key: String): Users? {
        Logger.d(TAG, "Read from room data source")
        return usersDao.getAll().mapToUsers()
    }

    override fun save(key: String, input: UsersRes) {
        Logger.d(TAG, "Save in room data source")
        usersDao.insertUsers(input.mapToEntities())
    }

    override fun clear(key: String) {
        usersDao.deleteAll()
    }
}

