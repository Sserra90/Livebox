package com.creations.convert_jackson.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.lang.reflect.Type;

/**
 * @author Sérgio Serra on 11/09/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class Util {

    public static <T> Type fromRef(TypeReference<T> reference) {
        return TypeFactory.defaultInstance().constructType(reference);
    }

    public static class TypeRef<T> extends TypeReference<T> {
    }

}
