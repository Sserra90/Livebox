package com.creations.livebox.rx;

import android.util.Log;

import com.creations.livebox.Logger;

import org.reactivestreams.Publisher;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.functions.Function;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 * <p>
 * Taken from # https://stackoverflow.com/questions/22066481/rxjava-can-i-use-retry-but-with-delay
 */
public class RetryWithDelay implements Function<Flowable<Throwable>, Publisher<?>> {

    private static final String TAG = "RetryWithDelay";

    private final int maxRetries;
    private final long retryDelayMillis;
    private int retryCount;

    public RetryWithDelay(final int maxRetries, final long retryDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.retryCount = 0;
    }

    @Override
    public Publisher<?> apply(Flowable<Throwable> attempts) {
        return attempts.flatMap((Function<Throwable, Flowable<?>>) throwable -> {
            if (++retryCount < maxRetries) {
                Logger.d(TAG, "Retry for the " + retryCount + " time");
                // When this Observable calls onNext, the original
                // Observable will be retried (i.e. re-subscribed).
                return Flowable.timer(retryDelayMillis, TimeUnit.MILLISECONDS);
            }

            // Max retries hit. Just pass the error along.
            return Flowable.error(throwable);
        });
    }

}
