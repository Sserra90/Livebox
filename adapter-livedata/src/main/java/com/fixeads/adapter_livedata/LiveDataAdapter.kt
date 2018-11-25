package com.fixeads.adapter_livedata

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import com.creations.livebox_common.adapter.ObservableAdapter
import com.creations.runtime.state.State
import com.creations.runtime.state.Status
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject


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
        val subject: Subject<State<T>> =
                BehaviorSubject.createDefault(State<T>(null, Status.Loading)).toSerialized()

        observable
                .subscribeOn(Schedulers.io())
                .subscribe({
                    subject.onNext(State(it, Status.Success))
                }, {
                    subject.onNext(State(null, Status.Error))
                })

        return subject
    }
}
