package com.creations.livebox;

import com.creations.livebox.converters.Converter;
import com.creations.livebox.datasources.DiskLruDataSource;
import com.creations.livebox.datasources.LocalDataSource;
import com.creations.livebox.datasources.RemoteDataSource;
import com.creations.livebox.rx.Transformers;
import com.creations.livebox.util.Objects;
import com.creations.livebox.util.Optional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.schedulers.Schedulers;

/**
 * @author Sérgio Serra
 * Criations
 * sergioserra99@gmail.com
 */
public class Livebox<RemoteData, Output> {

    private static final String TAG = "Livebox";
    // Keeps a record of in-flight requests.
    private static final ConcurrentHashMap<BoxKey, Observable> inFlightRequests = new ConcurrentHashMap<>();
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
    private RemoteDataSource<Observable<RemoteData>> mRemoteDataSource;
    // Local data sources
    private List<LocalDataSource<RemoteData, ?>> mLocalSources = new ArrayList<>();
    // Transformer that adds share functionality to an observable
    private ObservableTransformer<Output, Output> withShare = new ObservableTransformer<Output, Output>() {
        @Override
        public ObservableSource<Output> apply(Observable<Output> upstream) {
            Logger.d(TAG, "Compose with share");
            Observable<Output> observable = upstream
                    .doOnTerminate(() -> {
                        Logger.d(TAG, "Remove from inFlightRequests with key: " + mKey);
                        inFlightRequests.remove(mKey);
                    })
                    .share();

            inFlightRequests.putIfAbsent(mKey, observable);
            return observable;
        }
    };

    private Map<Class<?>, Converter<Output>> mConvertersMap = new HashMap<>();

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

    public static <RemoteSourceResult, Output> Livebox<RemoteSourceResult, Output> build(BoxKey key) {
        if (!mInit) {
            throw new IllegalStateException("Init must be called before using Livebox");
        }
        return new Livebox<>(key);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void checkPreconditions() {
    }


    public Livebox<RemoteData, Output> keepDataFresh() {
        mRefresh = true;
        return this;
    }

    public Livebox<RemoteData, Output> ignoreCache(boolean ignore) {
        mIgnoreDiskCache = ignore;
        return this;
    }

    public Livebox<RemoteData, Output> retryOnFailure() {
        mRetryOnFailure = true;
        return this;
    }

    public Livebox<RemoteData, Output> remoteSource(RemoteDataSource<Observable<RemoteData>> source) {
        mRemoteDataSource = source;
        return this;
    }

    public Livebox<RemoteData, Output> addLocalSource(LocalDataSource<RemoteData, ?> source) {
        mLocalSources.add(source);
        return this;
    }

    public Livebox<RemoteData, Output> addConverter(Class<?> aClass, Converter<Output> converter) {
        mConvertersMap.put(aClass, converter);
        return this;
    }


    private Observable<Optional<?>> loadFromLocalSource() {
        Logger.d(TAG, "loadFromLocalSource() called");

        if (mLocalSources.isEmpty()) {
            throw new IllegalStateException("No local source found");
        }

        return Observable.fromIterable(mLocalSources)
                .map((Function<LocalDataSource<RemoteData, ?>, Optional<?>>) source -> {
                    Logger.d(TAG, "---> Hit source " + source);
                    return source.read();
                })
                .filter(Optional::isPresent)
                .first(Optional.ofNullable(null))
                .doOnSuccess(optional -> {
                    if (optional.isPresent()) {
                        Logger.d(TAG, "---> Found valid data");
                        return;
                    }
                    Logger.d(TAG, "---> No valid data found");
                })
                .toObservable();
    }

    // Maps data from local data source type -> output type
    private Observable<Output> returnLocalData(Object localData) throws Exception {
        Logger.d(TAG, "returnLocalData() called with: localData = [" + localData + "]");
        return Observable.just(convert(localData));
    }

    // Fetch data from remote data source and pass new data to local sources.
    private Observable<Output> fetchFromRemoteDataSourceAndSave() {
        Logger.d(TAG, "fetchFromRemoteDataSourceAndSave() called");
        return Observable
                .defer(mRemoteDataSource::fetch)
                .doOnNext(this::passRemoteDataToLocalSources)
                .compose(Transformers.withRetry(mRetryOnFailure))
                .map(this::convert);
    }

    private Output convert(Object o) throws Exception {
        Converter<Output> converter = mConvertersMap.get(o.getClass());
        if (Objects.nonNull(converter)) {
            Optional<Output> data = converter.convert(o);
            Logger.d(TAG, "---> Converter found for type: " + o.getClass());
            if (data.isAbsent()) {
                throw new IllegalStateException("Converter: " + converter + "returned null for: " + o);
            }
            return data.get();
        }

        // If no converter was found, we try to cast because remoteData type parameter
        // could have the same type as output type parameter, in that case no converter is needed.
        //noinspection unchecked
        return (Output) o;
    }

    private void passRemoteDataToLocalSources(RemoteData data) {
        Logger.d(TAG, "\n");
        Logger.d(TAG, "Pass fresh data to local sources");
        for (LocalDataSource<RemoteData, ?> localSource : mLocalSources) {
            Logger.d(TAG, "---> Saving fresh data in: " + localSource);
            localSource.save(data);
        }
    }

    private Observable<Output> fetchFromRemoteDataSource() {
        return mRemoteDataSource.fetch()
                .map(this::convert)
                .compose(Transformers.withRetry(mRetryOnFailure))
                .compose(withShare);
    }

    public Observable<Output> asObservable() {

        // Check if we have an in-flight request ongoing.
        // If we do return the Observable so the caller can subscribe to it.
        if (inFlightRequests.get(mKey) != null) {
            Logger.d(TAG, "---> We have a in-flight request for key: " + mKey);
            //noinspection unchecked
            return (Observable<Output>) inFlightRequests.get(mKey);
        }

        // If ignore disk cache is true always hit remote data source
        if (mIgnoreDiskCache) {
            Logger.d(TAG, "Ignore disk cache, hit remote data source");
            return fetchFromRemoteDataSource();
        }

        // Get data from local source.
        Observable<Output> retObservable = loadFromLocalSource()
                .flatMap((Function<Optional<?>, Observable<Output>>) localResult -> {

                    // Local data is invalid, return a Observable that fetches remote data and
                    // saves to local data source.
                    if (localResult.isAbsent()) {
                        Logger.d(TAG, "Local data is invalid, hit remote data source and save");
                        return fetchFromRemoteDataSourceAndSave();
                    }

                    // At this point we know we have valid local data,
                    // if the user does not want to refresh return, otherwise return a Observable
                    // that emits local data, fetches the latest data from remote source and saves it.
                    if (!mRefresh) {
                        Logger.d(TAG, "Local data is valid, do not hit remote data source");
                        return returnLocalData(localResult.get());
                    } else {
                        Logger.d(TAG, "Local data is valid but still hit remote data source to refresh data");
                        return Observable.concat(
                                returnLocalData(localResult.get()),
                                fetchFromRemoteDataSourceAndSave()
                        );
                    }
                });

        // Using share to avoid multiple requests to be executed.
        // #see https://stackoverflow.com/questions/35951942/single-observable-with-multiple-subscribers/35952390#35952390
        return retObservable.compose(withShare);
    }

    // Convenience method to return an Observable that observes on Android main thread
    // and subscribes on IO scheduler.
    public Observable<Output> asAndroidObservable() {
        return asObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
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