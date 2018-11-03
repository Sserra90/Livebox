package com.creations.livebox.rx;

import com.creations.livebox.Box.RetryStrategy;
import com.creations.livebox_common.util.Logger;

import io.reactivex.ObservableTransformer;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class Transformers {

    private static final String TAG = "Transformers";

    public static <T> ObservableTransformer<T, T> withRetry(boolean retry, RetryStrategy strategy) {
        return upstream -> {
            if (retry) {
                Logger.d(TAG, "Compose with retry strategy: " + strategy);
                return upstream.retryWhen(
                        strategy == RetryStrategy.INTERVAL ? new RetryWithDelay() : new BackoffRetry()
                );
            }
            return upstream;
        };
    }
}
