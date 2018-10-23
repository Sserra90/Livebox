package com.creations.livebox.adapters

import com.creations.livebox_common.adapter.ObservableAdapter
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class AndroidAdapter<T> : ObservableAdapter<T, Observable<T>> {
    private val subscribeScheduler: Scheduler
    private val observerScheduler: Scheduler

    interface SchedulerProvider {
        fun subscribe(): Scheduler
        fun observer(): Scheduler
    }

    init {
        subscribeScheduler = if (schedulerProvider == null) Schedulers.io() else schedulerProvider!!.subscribe()
        observerScheduler = if (schedulerProvider == null) AndroidSchedulers.mainThread() else schedulerProvider!!.observer()
    }

    override fun adapt(observable: Observable<T>): Observable<T> = observable
            .subscribeOn(subscribeScheduler)
            .observeOn(observerScheduler)

    companion object {
        private var schedulerProvider: SchedulerProvider? = null

        @JvmStatic
        fun setSchedulerProvider(schedulerProvider: SchedulerProvider) {
            AndroidAdapter.schedulerProvider = schedulerProvider
        }
    }
}
