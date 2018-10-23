package com.fixeads.adapter_autodispose

import com.creations.livebox_common.adapter.ObservableAdapter
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.ObservableSubscribeProxy
import com.uber.autodispose.lifecycle.LifecycleScopeProvider

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * @author Sérgio Serra on 09/09/2018.
 * sergioserra99@gmail.com
 */
class AutoDisposeAdapter<T> private constructor(
        private val mLifecycleScope: LifecycleScopeProvider<*>,
        private val mObserveOnMainThread: Boolean
) : ObservableAdapter<T, ObservableSubscribeProxy<T>> {

    override fun adapt(obs: Observable<T>): ObservableSubscribeProxy<T> {
        var observable = obs

        if (mObserveOnMainThread) {
            observable = observable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
        }

        return observable.`as`(AutoDispose.autoDisposable(mLifecycleScope))
    }

    companion object {
        @JvmStatic
        fun <T> of(mLifecycleScope: LifecycleScopeProvider<*>, android: Boolean = false)
                : ObservableAdapter<T, ObservableSubscribeProxy<T>> = AutoDisposeAdapter(mLifecycleScope, android)
    }

}
