package com.creations.livebox.rx;

import com.creations.livebox.util.Logger;

import java.util.concurrent.TimeUnit;

import io.reactivex.ObservableTransformer;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class Transformers {

    private static final String TAG = "Transformers";

    public static <T> ObservableTransformer<T, T> withRetry(boolean retry) {
        return upstream -> {
            if (retry) {
                Logger.d(TAG, "Compose with retry");
                return upstream.retryWhen(new RetryWithDelay(3, TimeUnit.SECONDS.toMillis(2)));
            }
            return upstream;
        };
    }
}
