package com.creations.livebox.adapters;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * @author Sérgio Serra on 27/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class LiveDataAdapter<T> implements ObservableAdapter<T, LiveData<T>> {

    @Override
    public LiveData<T> adapt(Observable<T> observable) {
        MutableLiveData<T> liveData = new MutableLiveData<>();
        observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<T>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(T t) {
                        liveData.setValue(t);
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });
        return liveData;
    }

}
