package com.creations.livebox.adapters;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * @author Sérgio Serra on 27/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class LiveDataAdapter<T> implements ObservableAdapter<T, LiveData<T>> {

    @Override
    public LiveData<T> adapt(Observable<T> observable) {
        MutableLiveData<T> liveData = new MutableLiveData<>();
        observable.subscribe(new Observer<T>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(T t) {
                liveData.postValue(t);
            }

            @Override
            public void onError(Throwable e) {
                liveData.postValue(null);
            }

            @Override
            public void onComplete() {
            }
        });
        return liveData;
    }

}
