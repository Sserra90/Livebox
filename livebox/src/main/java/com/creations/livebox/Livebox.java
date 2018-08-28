package com.creations.livebox;

import android.arch.lifecycle.LiveData;

import com.creations.livebox.adapters.LiveDataAdapter;
import com.creations.livebox.adapters.ObservableAdapter;
import com.creations.livebox.converters.Converter;
import com.creations.livebox.converters.ConvertersFactory;
import com.creations.livebox.datasources.DataSourceFactory;
import com.creations.livebox.datasources.DiskLruDataSource;
import com.creations.livebox.datasources.LiveboxDataSourceFactory;
import com.creations.livebox.datasources.LocalDataSource;
import com.creations.livebox.datasources.RemoteDataSource;
import com.creations.livebox.rx.Transformers;
import com.creations.livebox.util.Objects;
import com.creations.livebox.util.Optional;
import com.creations.livebox.validator.Validator;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
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
import io.reactivex.annotations.NonNull;
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
    private RemoteDataSource<RemoteData> mRemoteDataSource;
    // Local data sources
    private List<LocalDataSource<RemoteData, ?>> mLocalSources = new ArrayList<>();
    // Stores validator for each store instance
    private Map<LocalDataSource<RemoteData, ?>, Validator> mValidators = new HashMap<>();

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

    private Map<Class<?>, Converter<?, Output>> mConvertersMap = new HashMap<>();
    private Optional<ConvertersFactory<Output>> mConverterFactory = Optional.empty();

    private List<DataSourceFactory<RemoteData>> mDataSourceFactoryList = new ArrayList<>();

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public Livebox(String key) {
        ObjectHelper.requireNonNull(key, "Key cannot be null");
        mKey = new BoxKey(key);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void init(DiskLruDataSource.Config diskCacheConfig) {
        ObjectHelper.requireNonNull(diskCacheConfig, "Cache config cannot be null");
        DiskLruDataSource.setConfig(diskCacheConfig);
        mInit = true;
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

    public Livebox<RemoteData, Output> fetch(RemoteDataSource<RemoteData> source, TypeToken type) {
        fetch(source, type.getType());
        return this;
    }

    public Livebox<RemoteData, Output> fetch(RemoteDataSource<RemoteData> source, Type type) {
        mRemoteDataSource = source;
        mDataSourceFactoryList.add(new LiveboxDataSourceFactory<>(type));
        return this;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public <T> Livebox<RemoteData, Output> addSource(LocalDataSource<RemoteData, T> source, Validator<T> validator) {
        ObjectHelper.requireNonNull(validator, "Validator cannot be null");
        mLocalSources.add(source);
        mValidators.put(source, validator);
        return this;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public <T> Livebox<RemoteData, Output> addSource(int dataSourceId, Validator<T> validator) {
        ObjectHelper.requireNonNull(validator, "Validator cannot be null");
        for (DataSourceFactory<RemoteData> factory : mDataSourceFactoryList) {
            Optional<LocalDataSource<RemoteData, T>> localDataSource = factory.get(dataSourceId);
            if (localDataSource.isPresent()) {
                addSource(localDataSource.get(), validator);
                break;
            }
        }
        return this;
    }

    public Livebox<RemoteData, Output> addLocalSourceFactory(DataSourceFactory<RemoteData> dataSourceFactory) {
        mDataSourceFactoryList.add(dataSourceFactory);
        return this;
    }

    public <T> Livebox<RemoteData, Output> addConverter(Class<T> aClass, Converter<T, Output> converter) {
        mConvertersMap.put(aClass, converter);
        return this;
    }

    public Livebox<RemoteData, Output> addConverterFactory(ConvertersFactory<Output> converterFactory) {
        mConverterFactory = Optional.ofNullable(converterFactory);
        return this;
    }


    private Observable<Optional<?>> loadFromLocalSource() {
        Logger.d(TAG, "loadFromLocalSource() called");

        if (mLocalSources.isEmpty()) {
            throw new IllegalStateException("No local sources found");
        }

        return Observable.fromIterable(mLocalSources)
                .map(source -> {
                    Logger.d(TAG, "---> Hit source " + source);
                    final Optional<?> data = source.read(mKey.key());
                    @SuppressWarnings("unchecked")
                    boolean isValid = data.isPresent() && mValidators.get(source).validate(data.get());
                    Logger.d(TAG, "---> Data from source " + source + " is valid: " + isValid);
                    return isValid ? data : Optional.empty();
                })
                .filter(Optional::isPresent)
                .first(Optional.empty())
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

    @SuppressWarnings("unchecked")
    private <T> Output convert(T data) throws Exception {

        Converter<T, Output> converter;
        if (mConverterFactory.isPresent()) {
            Logger.d(TAG, "---> Using converter factory");
            converter = mConverterFactory.get().get((Class<T>) data.getClass());
        } else {
            converter = (Converter<T, Output>) mConvertersMap.get(data.getClass());
        }

        if (Objects.nonNull(converter)) {
            Optional<Output> convertedData = converter.convert(data);
            Logger.d(TAG, "---> Converter found for type: " + data.getClass());
            if (convertedData.isAbsent()) {
                throw new IllegalStateException("Converter: " + converter + "returned null for: " + data);
            }
            return convertedData.get();
        }

        // If no converter was found, we try casting because remoteData type parameter
        // could have the same type as output type parameter, in that case no converter is needed.
        //noinspection unchecked
        return (Output) data;
    }

    private void passRemoteDataToLocalSources(RemoteData data) {
        Logger.d(TAG, "\n");
        Logger.d(TAG, "Pass fresh data to local sources");
        for (LocalDataSource<RemoteData, ?> localSource : mLocalSources) {
            Logger.d(TAG, "---> Saving fresh data in: " + localSource);
            localSource.save(mKey.key(), data);
        }
    }

    private Observable<Output> fetchFromRemoteDataSource() {
        return mRemoteDataSource.fetch()
                .map(this::convert)
                .compose(Transformers.withRetry(mRetryOnFailure))
                .compose(withShare);
    }

    public Observable<Output> asObservable() {

        // Check if we have a request ongoing.
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

    // Convenience method that returns a live data instance.
    public LiveData<Output> asLiveData() {
        return new LiveDataAdapter<Output>().adapt(asObservable());
    }

    // Uses passed adapter to adapt the result observable.
    public <T> T as(@NonNull ObservableAdapter<Output, T> adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("Adapter cannot be null");
        }
        return adapter.adapt(asObservable());
    }

    // A Key that uses a single string as identifier
    // Key must match the regex [a-z0-9_-]{1,120}.
    private static class BoxKey {

        private static final String STRING_KEY_PATTERN = "[a-z0-9_-]{1,120}";
        private static final Pattern LEGAL_KEY_PATTERN = Pattern.compile(STRING_KEY_PATTERN);

        private String mKey;

        BoxKey(String key) {
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