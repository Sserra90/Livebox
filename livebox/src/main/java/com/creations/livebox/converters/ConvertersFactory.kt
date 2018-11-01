package com.creations.livebox.converters

/**
 * @author Sérgio Serra on 26/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
interface ConvertersFactory<R> {
    operator fun <T> get(aClass: Class<T>): Converter<T, R>
}
