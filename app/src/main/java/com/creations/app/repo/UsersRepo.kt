package com.creations.app.repo

import com.creations.app.api.Api
import com.creations.app.api.GithubService
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
import com.creations.runtime.state.Status.Success
import com.fixeads.adapter_livedata.StateAdapter
import com.sserra.annotations.Assets
import com.sserra.livebox_jackson.box
import io.reactivex.Observable
import java.lang.reflect.Type

object Keys {
    const val GET_USERS = "get_users"
}

object RepoFactory {
    fun provideRepo(status: Status = Success, test: Boolean = false): UsersRepo =
            if (test) FakeUsersRepo(status) else RemoteUsersRepo()
}

@Assets(folder = "users", mapsTo = UsersRes::class)
interface UsersRepo {
    val getUsers: Observable<State<Users>>
}

class FakeUsersRepo(private val status: Status = Success) : UsersRepo {

    override val getUsers: Observable<State<Users>>
        get() = box<UsersRes, Users>()
                .withKey(Keys.GET_USERS)
                .fetch(usersFetchers(status))
                .addConverter<UsersRes> { Users.fromUsersRes(it) }
                .toStateAdapter()
}

class RemoteUsersRepo(private val api: GithubService = Api.getInstance().githubService) : UsersRepo {

    override val getUsers: Observable<State<Users>>
        get() = box<UsersRes, Users>()
                .withKey(Keys.GET_USERS)
                .fetch { api.userList }
                .addSource(Sources.DISK_LRU, 1.minutes<UsersRes>())
                .addSource(UsersRoomDataSource()) { _, users -> users.items.isNotEmpty() }
                .addConverter<UsersRes> { Users.fromUsersRes(it) }
                .retryOnFailure()
                .toStateAdapter()

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

fun <I, O> Box<I, O>.toStateAdapter(): Observable<State<O>> = build().adapt(StateAdapter())

//fun usersFetchers(status: Status): Fetcher<UsersRes> = UsersAssets().usersFetchersMap[status]!!

/*
class UsersAssets {

    val usersFetchersMap: Map<Status, Fetcher<UsersRes>> by lazy {
        mapOf<Status, Fetcher<UsersRes>>(
                Success to assetFetcher("users/users_success.json"),
                NoResults to assetFetcher("users/users_noResults.json"),
                Error to errorFetcher()
        )
    }

    val usersErrorResponse: String by bindAsset("users/users_error.json")
    val usersNoResultsResponse: String by bindAsset("users/users_noResults.json")
    val usersSuccessResponse: String by bindAsset("users/users_success.json")
}*/