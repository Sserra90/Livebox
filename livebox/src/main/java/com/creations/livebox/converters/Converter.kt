package com.creations.livebox.converters

import io.reactivex.annotations.NonNull

/**
 * @author Sérgio Serra on 26/08/2018.
 * sergioserra99@gmail.com
 */
interface Converter<R, T> {
    @NonNull
    @Throws(Exception::class)
    fun convert(o: R): T
}
