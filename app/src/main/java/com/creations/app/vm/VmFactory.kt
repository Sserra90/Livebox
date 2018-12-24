package com.creations.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.creations.app.repo.RepoFactory


/**
 * @author Sérgio Serra
 * sergioserra99@gmail.com
 */
class VmFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (UsersVm::class.java.isAssignableFrom(modelClass)) {
            @Suppress("UNCHECKED_CAST")
            return UsersVm(RepoFactory.provideRepo(test = true)) as T
        }
        throw IllegalArgumentException()
    }
}