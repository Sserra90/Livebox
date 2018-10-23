package com.fixeads.adapter_livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams

import com.creations.livebox_common.adapter.ObservableAdapter

import io.reactivex.BackpressureStrategy
import io.reactivex.Observable

/**
 * @author Sérgio Serra.
 * Converts an [Observable] to [LiveData] instance using [LiveDataReactiveStreams]
 */
class LiveDataAdapter<T> : ObservableAdapter<T, LiveData<T>> {
    override fun adapt(observable: Observable<T>): LiveData<T> =
            LiveDataReactiveStreams.fromPublisher(observable.toFlowable(BackpressureStrategy.BUFFER))
}
