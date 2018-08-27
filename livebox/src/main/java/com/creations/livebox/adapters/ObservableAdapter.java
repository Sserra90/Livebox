package com.creations.livebox.adapters;

import io.reactivex.Observable;

/**
 * @author Sérgio Serra on 27/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public interface ObservableAdapter<T, R> {
    R adapt(Observable<T> observable);
}
