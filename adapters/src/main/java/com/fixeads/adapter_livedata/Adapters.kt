package com.fixeads.adapter_livedata

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import com.creations.livebox_common.adapter.ObservableAdapter
import com.creations.runtime.state.*
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.ObservableSubscribeProxy
import com.uber.autodispose.lifecycle.LifecycleScopeProvider
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.Observable.concat
import io.reactivex.Observable.just


/**
 * @author Sérgio Serra.
 * Converts an [Observable] to [LiveData] instance using [LiveDataReactiveStreams]
 */
class LiveDataAdapter<T> : ObservableAdapter<T, LiveData<T>> {
    override fun adapt(observable: Observable<T>): LiveData<T> =
            LiveDataReactiveStreams.fromPublisher(observable.toFlowable(BackpressureStrategy.BUFFER))
}

class StateAdapter<T> : ObservableAdapter<T, Observable<State<T>>> {
    @SuppressLint("CheckResult")
    override fun adapt(observable: Observable<T>): Observable<State<T>> {
        return concat(
                just(loading()),
                observable
                        .map { success(it) }
                        .onErrorReturn { error(StateError(it)) }
        )
    }
}

class AutoDisposeAdapter<T> private constructor(
        private val mLifecycleScope: LifecycleScopeProvider<*>
) : ObservableAdapter<T, ObservableSubscribeProxy<T>> {

    override fun adapt(observable: Observable<T>): ObservableSubscribeProxy<T> =
            observable.`as`(AutoDispose.autoDisposable(mLifecycleScope))

    companion object {
        @JvmStatic
        fun <T> of(lifecycleScope: LifecycleScopeProvider<*>)
                : ObservableAdapter<T, ObservableSubscribeProxy<T>> = AutoDisposeAdapter(lifecycleScope)
    }

}
