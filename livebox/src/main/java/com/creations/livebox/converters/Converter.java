package com.creations.livebox.converters;

import com.creations.livebox.util.Optional;

import io.reactivex.annotations.NonNull;

/**
 * @author Sérgio Serra on 26/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public interface Converter<R, T> {
    @NonNull
    Optional<T> convert(R o) throws Exception;
}
