package com.creations.livebox.rx;

import android.util.Log;

import org.reactivestreams.Publisher;

import java.util.concurrent.TimeUnit;

import io.reactivex.FlowableTransformer;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class Transformers {

    private static final String TAG = "Transformers";

    public static <T> FlowableTransformer<T, T> withRetry(boolean retry) {
        return upstream -> {
            if (retry) {
                Log.d(TAG, "Compose with retry");
                return upstream.retryWhen(new RetryWithDelay(3, TimeUnit.SECONDS.toMillis(2)));
            }
            return upstream;
        };
    }
}
