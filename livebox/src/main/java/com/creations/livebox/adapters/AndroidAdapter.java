package com.creations.livebox.adapters;

import com.creations.livebox_common.adapter.ObservableAdapter;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class AndroidAdapter<T> implements ObservableAdapter<T, Observable<T>> {

    public interface SchedulerProvider {
        Scheduler subscribe();

        Scheduler observer();
    }

    private static SchedulerProvider schedulerProvider;
    private Scheduler subscribeScheduler;
    private Scheduler observerScheduler;

    public AndroidAdapter() {
        subscribeScheduler = schedulerProvider == null ? AndroidSchedulers.mainThread() : schedulerProvider.subscribe();
        observerScheduler = schedulerProvider == null ? Schedulers.io() : schedulerProvider.observer();
    }

    public static void setSchedulerProvider(SchedulerProvider schedulerProvider) {
        AndroidAdapter.schedulerProvider = schedulerProvider;
    }

    @Override
    public Observable<T> adapt(Observable<T> observable) {
        return observable
                .subscribeOn(subscribeScheduler)
                .observeOn(observerScheduler);
    }
}
