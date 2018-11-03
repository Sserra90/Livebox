package com.creations.app.room

import androidx.room.*
import com.creations.app.api.UsersRes
import com.creations.app.entities.Users

fun UsersRes.mapToEntities(): List<UserEntity> {
    if (items.isEmpty()) return listOf()
    return items.map { UserEntity().apply { id = it.id } }
}

fun List<UserEntity>.mapToUsers(): Users = Users(map { it.id })

@Entity(tableName = "users")
class UserEntity {
    @PrimaryKey
    var id: Int = 0
}

@Dao
interface UsersDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUsers(user: List<UserEntity>)

    @Query("DELETE FROM users")
    fun deleteAll()

    @Query("SELECT * FROM users")
    fun getAll(): List<UserEntity>
}