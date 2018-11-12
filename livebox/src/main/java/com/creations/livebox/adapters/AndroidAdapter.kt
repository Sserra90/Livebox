package com.creations.livebox.adapters

import com.creations.livebox_common.adapter.ObservableAdapter
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class AndroidAdapter<T> : ObservableAdapter<T, Observable<T>> {
    override fun adapt(observable: Observable<T>): Observable<T> = observable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
}
