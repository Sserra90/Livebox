package com.creations.app.entities

import com.creations.app.api.UsersRes
import io.reactivex.annotations.NonNull

/**
 * @author Sérgio Serra
 * Dummy representation of Users in domain layer.
 */
data class Users(val items: List<Int>) {
    companion object {
        @NonNull
        fun fromUsersRes(usersRes: UsersRes) = Users(usersRes.items.map { it.id })
    }
}
