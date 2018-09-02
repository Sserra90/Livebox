package com.creations.livebox.validator;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public interface Validator<R> {
    boolean validate(String key, R item);
}
