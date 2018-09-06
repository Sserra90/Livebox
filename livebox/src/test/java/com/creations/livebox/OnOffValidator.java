package com.creations.livebox;

import com.creations.livebox.validator.Validator;

/**
 * @author Sérgio Serra on 06/09/2018.
 * Criations
 * sergioserra99@gmail.com
 *
 * Dummmy on/off validator used for testing data sources.
 */
class OnOffValidator<T> implements Validator<T> {
    private boolean on;

    OnOffValidator(boolean on) {
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
