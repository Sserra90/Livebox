package com.creations.app.vm

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.creations.app.repo.UsersRepo
import com.fixeads.adapter_autodispose.AutoDisposeViewModel
import com.fixeads.adapter_autodispose.autoDispose

class UsersVm(private val usersRepo: UsersRepo) : AutoDisposeViewModel() {

    companion object {
        const val TAG = "UsersVm"
    }

    val usersLiveData: MutableLiveData<List<Int>> = MutableLiveData()

    fun getUsers() {
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