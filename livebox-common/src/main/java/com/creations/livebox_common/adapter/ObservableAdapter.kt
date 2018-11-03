package com.creations.livebox_common.adapter

import io.reactivex.Observable

/**
 * @author Sérgio Serra on 27/08/2018.
 * sergioserra99@gmail.com
 */
interface ObservableAdapter<T, R> {
    fun adapt(observable: Observable<T>): R
}
