package com.creations.livebox.validator;

import com.creations.livebox.Livebox;
import com.creations.livebox.util.Optional;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author Sérgio Serra on 01/09/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public final class AgeValidator<R> implements Validator<R> {

    // Time to live before expiring the resources
    private long mTtl;
    private Journal mJournal;

    public static <T> AgeValidator<T> create(long ttl) {
        return new AgeValidator<>(Livebox.journal, ttl);
    }

    public static <T> AgeValidator<T> minutes(int minutes) {
        return create(MINUTES.toMillis(minutes));
    }

    public static <T> AgeValidator<T> hours(int hours) {
        return create(HOURS.toMillis(hours));
    }

    public static <T> AgeValidator<T> days(int days) {
        return create(DAYS.toMillis(days));
    }

    private AgeValidator(Journal journal, long ttl) {
        mJournal = journal;
        mTtl = ttl;
    }

    @Override
    public boolean validate(String key, R item) {
        Optional<Long> lastTimestampOpt = mJournal.read(key);
        // By default if no timestamp is available resource is valid
        return lastTimestampOpt.isAbsent() ||
                lastTimestampOpt.get() + mTtl >= System.currentTimeMillis();
    }
}
