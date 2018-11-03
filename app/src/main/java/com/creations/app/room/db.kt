package com.creations.app.room

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [UserEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usersDao(): UsersDao
}

@SuppressLint("StaticFieldLeak")
object Db {
    private const val name = "livebox-db"
    var appContext: Context? = null

    private val db: AppDatabase by lazy {
        if (appContext == null) {
            throw IllegalStateException()
        }
        Room.databaseBuilder<AppDatabase>(appContext!!, AppDatabase::class.java, name).build()
    }

    fun usersDao() = db.usersDao()
}
