package com.creations.livebox.validator

/**
 * @author Sérgio Serra on 25/08/2018.
 * sergioserra99@gmail.com
 */
interface Validator<R> {
    fun validate(key: String, item: R): Boolean
}
