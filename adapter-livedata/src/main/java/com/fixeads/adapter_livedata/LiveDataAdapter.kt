package com.fixeads.adapter_livedata

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import com.creations.livebox_common.adapter.ObservableAdapter
import com.creations.runtime.state.State
import com.creations.runtime.state.error
import com.creations.runtime.state.loading
import com.creations.runtime.state.success
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.atomic.AtomicBoolean


/**
 * @author Sérgio Serra.
 * Converts an [Observable] to [LiveData] instance using [LiveDataReactiveStreams]
 */
class LiveDataAdapter<T> : ObservableAdapter<T, LiveData<T>> {
    override fun adapt(observable: Observable<T>): LiveData<T> =
            LiveDataReactiveStreams.fromPublisher(observable.toFlowable(BackpressureStrategy.BUFFER))
}

class StateAdapter<T> : ObservableAdapter<T, Observable<State<T>>> {

    val isSubscribed = AtomicBoolean(false)

    @SuppressLint("CheckResult")
    override fun adapt(observable: Observable<T>): Observable<State<T>> {
        val subject: Subject<State<T>> = BehaviorSubject.create<State<T>>().toSerialized()
        val obs = subject.doOnSubscribe { _ ->
            if (isSubscribed.compareAndSet(false, true)) {
                observable
                        .subscribeOn(Schedulers.io())
                        .doOnSubscribe { subject.onNext(loading()) }
                        .subscribe({
                            subject.onNext(success(it))
                        }, {
                            subject.onNext(error())
                        })
            }
        }
        return obs
    }
}
