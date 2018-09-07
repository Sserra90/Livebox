package com.creations.livebox.datasources.fetcher;

import io.reactivex.Observable;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public interface Fetcher<T> {
    Observable<T> fetch();
}
