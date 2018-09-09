package com.creations.livebox.adapters;

import com.uber.autodispose.AutoDispose;
import com.uber.autodispose.ObservableSubscribeProxy;
import com.uber.autodispose.lifecycle.LifecycleScopeProvider;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * @author Sérgio Serra on 09/09/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class AutoDisposeAdapter<T> implements ObservableAdapter<T, ObservableSubscribeProxy<T>> {

    private LifecycleScopeProvider mLifecycleScope;
    private boolean mObserveOnMainThread;

    private AutoDisposeAdapter(LifecycleScopeProvider mLifecycleScope, boolean mObserveOnMainThread) {
        this.mLifecycleScope = mLifecycleScope;
        this.mObserveOnMainThread = mObserveOnMainThread;
    }

    public static <T> ObservableAdapter<T, ObservableSubscribeProxy<T>> android(LifecycleScopeProvider mLifecycleScope) {
        return new AutoDisposeAdapter<>(mLifecycleScope, true);
    }

    public static <T> ObservableAdapter<T, ObservableSubscribeProxy<T>> of(LifecycleScopeProvider mLifecycleScope) {
        return new AutoDisposeAdapter<>(mLifecycleScope, false);
    }

    @Override
    public ObservableSubscribeProxy<T> adapt(Observable<T> observable) {

        if (mObserveOnMainThread) {
            observable = observable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        }

        return observable.as(AutoDispose.autoDisposable(mLifecycleScope));
    }
}
