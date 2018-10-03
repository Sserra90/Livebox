package com.creations.livebox.converters;

import io.reactivex.annotations.NonNull;

/**
 * @author Sérgio Serra on 26/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public interface Converter<R, T> {
    @NonNull
    T convert(R o) throws Exception;
}
