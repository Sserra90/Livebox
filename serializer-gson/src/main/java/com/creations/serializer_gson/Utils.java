package com.creations.serializer_gson;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

/**
 * @author Sérgio Serra on 11/09/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class Utils {
    public static <T> Type fromType(TypeToken<T> token) {
        return token.getType();
    }
}
