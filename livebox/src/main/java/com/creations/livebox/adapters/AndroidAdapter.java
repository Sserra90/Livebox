package com.creations.livebox.adapters;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class AndroidAdapter<T> implements ObservableAdapter<T, Observable<T>> {

    @Override
    public Observable<T> adapt(Observable<T> observable) {
        return observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
