package com.creations.app.vm

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.creations.app.entities.Users
import com.creations.app.repo.UsersRepo
import com.creations.runtime.state.State
import com.fixeads.adapter_livedata.AutoDisposeViewModel
import com.fixeads.adapter_livedata.autoDispose

class UsersVm(private val usersRepo: UsersRepo) : AutoDisposeViewModel() {

    companion object {
        const val TAG = "UsersVm"
    }

    val usersLiveData: MutableLiveData<Users> = MutableLiveData()
    val usersState: MutableLiveData<State<Users>> = MutableLiveData()

    fun getUsers() {
        /*usersRepo
                .usersState
                .autoDispose(this)
                .subscribe { usersState.value = it } */

        usersRepo
                .users
                .autoDispose(this)
                .subscribe(
                        { usersLiveData.value = it },
                        {},
                        { Log.d(TAG, "OnComplete") }
                )
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "OnCleared")
    }
}