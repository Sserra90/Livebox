package com.creations.livebox.serializers;

import okio.BufferedSource;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public interface Serializer<T> {
    BufferedSource serialize(T input);

    T deserialize(BufferedSource source);
}
