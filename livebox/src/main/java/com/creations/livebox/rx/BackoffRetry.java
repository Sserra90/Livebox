package com.creations.livebox.rx;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

/**
 * @author Sérgio Serra on 08/09/2018.
 * Criations
 * sergioserra99@gmail.com
 * <p>
 * Backoff retry implementation based on {#see https://gist.github.com/hzsweers/7902e3a0286774630f4f}
 */
public class BackoffRetry implements Function<Observable<Throwable>, Observable<?>> {

    private static final int MAX_RETRIES = 3;
    private int mMaxRetries;

    BackoffRetry() {
        mMaxRetries = MAX_RETRIES;
    }

    public BackoffRetry(int maxRetries) {
        mMaxRetries = maxRetries;
    }

    @Override
    public Observable<?> apply(Observable<Throwable> throwObs) {
        return throwObs.zipWith(Observable.range(1, MAX_RETRIES), (err, attempt) ->
                attempt < MAX_RETRIES ?
                        Observable.timer((long) Math.pow(4, attempt), TimeUnit.SECONDS) :
                        Observable.error(err))
                .flatMap(x -> x);
    }
}
