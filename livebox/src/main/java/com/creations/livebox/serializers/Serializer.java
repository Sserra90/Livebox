package com.creations.livebox.serializers;

import java.lang.reflect.Type;

import okio.BufferedSource;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public interface Serializer {
    <T> BufferedSource serialize(T input, Type type);

    <T> T deserialize(BufferedSource source, Type type);
}
