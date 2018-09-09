package com.creations.livebox.util;

import com.creations.livebox.validator.Validator;

/**
 * @author Sérgio Serra on 06/09/2018.
 * Criations
 * sergioserra99@gmail.com
 * <p>
 * Dummmy on/off validator used for testing data sources.
 */
public class OnOffValidator<T> implements Validator<T> {
    private boolean on;

    public OnOffValidator(boolean on) {
        this.on = on;
    }

    @Override
    public boolean validate(String key, T item) {
        return on;
    }

    public void setOn(boolean on) {
        this.on = on;
    }
}
