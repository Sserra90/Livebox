package com.creations.app.repo

import com.creations.app.api.Api
import com.creations.app.api.GithubService
import com.creations.app.api.User
import com.creations.app.api.UsersRes
import com.creations.app.entities.Users
import com.creations.app.room.Db
import com.creations.app.room.UsersDao
import com.creations.app.room.mapToEntities
import com.creations.app.room.mapToUsers
import com.creations.livebox.Box
import com.creations.livebox.datasources.LocalDataSource
import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory.Sources
import com.creations.livebox.validator.minutes
import com.creations.livebox_common.util.Logger
import com.creations.runtime.state.State
import com.creations.runtime.state.Status
import com.creations.runtime.state.success
import com.fixeads.adapter_livedata.StateAdapter
import com.sserra.livebox_jackson.box
import com.sserra.livebox_jackson.fileFetcher
import io.reactivex.Observable
import java.lang.reflect.Type

object Keys {
    const val GET_USERS = "get_users"
}

object RepoFactory {
    fun provideRepo(status: Status = Status.Success, test: Boolean = false): UsersRepo =
            if (test) FakeUsersRepo(status) else RemoteUsersRepo()
}

interface UsersRepo {
    val usersState: Observable<State<Users>>
}

class FakeUsersRepo(private val status: Status = Status.Success) : UsersRepo {

    private val stateMap = mapOf(
            Status.Success to "resources/users_success.json",
            Status.Error to "resources/users_error.json",
            Status.NoResults to "resources/users_no_results.json"
    )

    override val usersState: Observable<State<Users>>
        get() = box<UsersRes, Users>()
                .withKey(Keys.GET_USERS)
                .fetch(fileFetcher(stateMap[status]!!))
                .addConverter<UsersRes> { Users.fromUsersRes(it) }
                .stateAdapter()
}

class RemoteUsersRepo(private val api: GithubService = Api.getInstance().githubService) : UsersRepo {

    override val usersState: Observable<State<Users>>
        get() = box<UsersRes, Users>()
                .withKey(Keys.GET_USERS)
                .fetch { api.userList }
                .addSource(Sources.DISK_LRU, 1.minutes<UsersRes>())
                .addSource(UsersRoomDataSource()) { _, users -> users.items.isNotEmpty() }
                .addConverter<UsersRes> { Users.fromUsersRes(it) }
                .retryOnFailure()
                .build()
                .adapt(StateAdapter())

    val users: Observable<Users>
        get() = box<UsersRes, Users>()
                .withKey("users")
                .fetch { api.userList }
                .addSource(Sources.DISK_LRU, 1.minutes<UsersRes>())
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

fun <I, O> Box<I, O>.stateAdapter(): Observable<State<O>> = build().adapt(StateAdapter())
