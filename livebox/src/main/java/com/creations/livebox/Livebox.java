package com.creations.livebox;

import android.util.Log;

import com.creations.livebox.datasources.DiskLruDataSource;
import com.creations.livebox.datasources.LocalDataSource;
import com.creations.livebox.datasources.RemoteDataSource;
import com.creations.livebox.rx.Transformers;
import com.creations.livebox.util.Optional;
import com.creations.livebox.validator.DataValidator;

import org.reactivestreams.Publisher;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * @author Sérgio Serra
 * Criations
 * sergioserra99@gmail.com
 */
public class Livebox<RemoteSourceResult, LocalSourceResult, Output> {

    private static final String TAG = "Livebox";
    // Keeps a record of in-flight requests.
    private static final ConcurrentHashMap<BoxKey, Flowable> inFlightRequests = new ConcurrentHashMap<>();
    // Is instance initialized
    private static boolean mInit = false;
    // A unique key that identifies this Livebox, used to keep track of in-flight requests.
    // Also this key is used to save and retrieve entries in cache
    private BoxKey mKey;
    // Indicates if we should make a fetch to remote data source even if the local data is still valid.
    private boolean mRefresh = false;
    // Indicates if we should ignore disk cache
    private boolean mIgnoreDiskCache = false;
    // Indicates if we should retry the remote data source request if an error occurs
    private boolean mRetryOnFailure = false;
    // Remote data source
    private RemoteDataSource<Flowable<RemoteSourceResult>> mRemoteDataSource;
    // Local data source
    private LocalDataSource<RemoteSourceResult, LocalSourceResult> mLocalDataSource;
    // Validator used to check if local data is still valid
    private DataValidator<LocalSourceResult> mDataValidator;
    // Mappers, used to map LocalSourceResult -> Output and RemoteSourceResult -> Output
    private Function<LocalSourceResult, Output> mLocalMapper;
    private Function<RemoteSourceResult, Output> mRemoteMapper;
    private FlowableTransformer<Output, Output> withShare = new FlowableTransformer<Output, Output>() {
        @Override
        public Publisher<Output> apply(Flowable<Output> upstream) {
            Log.d(TAG, "Compose with share");
            Flowable<Output> flowable = upstream
                    .doOnTerminate(() -> {
                        Log.d(TAG, "Remove from inFlightRequests with key: " + mKey);
                        inFlightRequests.remove(mKey);
                    })
                    .share();

            inFlightRequests.putIfAbsent(mKey, flowable);
            return flowable;
        }
    };

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Livebox(BoxKey key) {
        ObjectHelper.requireNonNull(key, "Key cannot be null");
        mKey = key;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void init(DiskLruDataSource.Config diskCacheConfig) {
        ObjectHelper.requireNonNull(diskCacheConfig, "Cache config cannot be null");
        DiskLruDataSource.setConfig(diskCacheConfig);
        mInit = true;
    }

    public static <RemoteSourceResult, LocalSourceResult, Output>
    Livebox<RemoteSourceResult, LocalSourceResult, Output> build(BoxKey key) {
        if (!mInit) {
            throw new IllegalStateException("Init must be called before using Livebox");
        }
        return new Livebox<>(key);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void checkPreconditions() {
        ObjectHelper.requireNonNull(mLocalMapper, "No local mapper was found");
        ObjectHelper.requireNonNull(mRemoteMapper, "No remote mapper was found");

        if (mLocalDataSource != null && mDataValidator == null) {
            throw new IllegalStateException("We need a validator to check local data freshness");
        }

    }


    public Livebox<RemoteSourceResult, LocalSourceResult, Output> keepDataFresh() {
        mRefresh = true;
        return this;
    }

    public Livebox<RemoteSourceResult, LocalSourceResult, Output> remoteDataSourceMapper(Function<RemoteSourceResult, Output> remoteMapper) {
        mRemoteMapper = remoteMapper;
        return this;
    }

    public Livebox<RemoteSourceResult, LocalSourceResult, Output> localDataSourceMapper(Function<LocalSourceResult, Output> localMapper) {
        mLocalMapper = localMapper;
        return this;
    }

    public Livebox<RemoteSourceResult, LocalSourceResult, Output> ignoreCache(boolean ignore) {
        mIgnoreDiskCache = ignore;
        return this;
    }

    public Livebox<RemoteSourceResult, LocalSourceResult, Output> retryOnFailure() {
        mRetryOnFailure = true;
        return this;
    }

    public Livebox<RemoteSourceResult, LocalSourceResult, Output> setDataValidator(DataValidator<LocalSourceResult> dataValidator) {
        mDataValidator = dataValidator;
        return this;
    }

    public Livebox<RemoteSourceResult, LocalSourceResult, Output> remoteDataSource(RemoteDataSource<Flowable<RemoteSourceResult>> source) {
        mRemoteDataSource = source;
        return this;
    }

    public Livebox<RemoteSourceResult, LocalSourceResult, Output> localDataSource(
            LocalDataSource<RemoteSourceResult, LocalSourceResult> source) {
        mLocalDataSource = source;
        return this;
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Flowable<Optional<LocalSourceResult>> loadFromLocalSource() {
        ObjectHelper.requireNonNull(mLocalDataSource, "Local data source cannot be null");
        Log.d(TAG, "loadFromLocalSource() called");
        return Flowable.fromCallable(() -> Optional.ofNullable(mLocalDataSource.read()));
    }

    // Check if local data is valid, if validator is null always return false.
    private boolean isLocalDataValid(LocalSourceResult localData) {
        Log.d(TAG, "isLocalDataValid() called with: localData = [" + localData + "]");
        return mDataValidator != null && mDataValidator.isValid(localData);
    }

    // Maps data from local data source type -> output type
    private Flowable<Output> returnFromLocalDataSource(LocalSourceResult localData) {
        Log.d(TAG, "returnFromLocalDataSource() called with: localData = [" + localData + "]");
        return Flowable.fromCallable(() -> mLocalMapper.apply(localData));
    }

    // Fetch data from remote data source.
    // After fetching, save in local data source and map to return type.
    private Flowable<Output> fetchFromRemoteDataSourceAndSave() {
        Log.d(TAG, "fetchFromRemoteDataSourceAndSave() called");
        return Flowable
                .defer(mRemoteDataSource::fetch)
                .doOnNext(mLocalDataSource::save)
                .compose(Transformers.withRetry(mRetryOnFailure))
                .map(mRemoteMapper);
    }

    private Flowable<Output> fetchFromRemoteDataSource() {
        return mRemoteDataSource.fetch()
                .compose(Transformers.withRetry(mRetryOnFailure))
                .map(mRemoteMapper);
    }

    public Flowable<Output> asFlowable() {
        //checkPreconditions();

        // Check if we have an in-flight request ongoing.
        // If we do return the flowable so the caller can subscribe to it.
        if (inFlightRequests.get(mKey) != null) {
            Log.d(TAG, "We have a in-flight request for key: " + mKey);
            //noinspection unchecked
            return inFlightRequests.get(mKey);
        }

        // If ignore disk cache is true always hit remote data source
        if (mIgnoreDiskCache) {
            Log.d(TAG, "Ignore disk cache, hit remote data source");
            return fetchFromRemoteDataSource().compose(withShare);
        }

        // Get data from local source.
        Flowable<Output> retFlowable = loadFromLocalSource()
                .flatMap((Function<Optional<LocalSourceResult>, Publisher<Output>>) localResultOpt -> {

                    // Check if the local data is valid
                    final boolean isValid = localResultOpt.isPresent() && isLocalDataValid(localResultOpt.get());

                    // Local data is invalid, return a Flowable that fetches remote data and
                    // saves to local data source.
                    if (!isValid) {
                        Log.d(TAG, "Local data is invalid, hit remote data source and save");
                        return fetchFromRemoteDataSourceAndSave();
                    }

                    // At this point we know we have valid local data,
                    // if the user does not want to refresh return, otherwise return a flowable
                    // that emits local data, fetches the latest data from remote source and saves it.
                    if (!mRefresh) {
                        Log.d(TAG, "Local data is valid, do not hit remote data source");
                        return returnFromLocalDataSource(localResultOpt.get());
                    } else {
                        Log.d(TAG, "Local data is valid but still hit remote data source to refresh data");
                        return Flowable.concat(
                                returnFromLocalDataSource(localResultOpt.get()),
                                fetchFromRemoteDataSourceAndSave()
                        );
                    }
                });

        // Using share to avoid multiple requests to be executed.
        // #see https://stackoverflow.com/questions/35951942/single-observable-with-multiple-subscribers/35952390#35952390
        return retFlowable.compose(withShare);
    }

    public Observable<Output> asObservable() {
        //checkPreconditions();
        return asFlowable().toObservable();
    }

    // A Key that uses a single string as identifier
    // Key must match the regex [a-z0-9_-]{1,120}.
    public static class BoxKey {

        private static final String STRING_KEY_PATTERN = "[a-z0-9_-]{1,120}";
        private static final Pattern LEGAL_KEY_PATTERN = Pattern.compile(STRING_KEY_PATTERN);

        private String mKey;

        public BoxKey(String key) {
            mKey = validateKey(key);
        }

        private String validateKey(String key) {
            Matcher matcher = LEGAL_KEY_PATTERN.matcher(key);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(
                        "keys must match regex " + STRING_KEY_PATTERN + ": \"" + key + "\"");
            }
            return key;
        }

        public String key() {
            return mKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BoxKey that = (BoxKey) o;
            return mKey.equals(that.mKey);
        }

        @Override
        public int hashCode() {
            return mKey.hashCode();
        }

        @Override
        public String toString() {
            return mKey;
        }
    }
}