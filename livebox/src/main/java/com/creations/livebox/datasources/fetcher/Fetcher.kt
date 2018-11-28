package com.creations.livebox.datasources.fetcher

import io.reactivex.Observable
import io.reactivex.Single

/**
 * @author Sérgio Serra on 25/08/2018.
 * sergioserra99@gmail.com
 */
interface Fetcher<T> {
    fun fetch(): Observable<T>
}