package com.creations.livebox;

import android.arch.lifecycle.LiveData;

import com.creations.livebox.adapters.LiveDataAdapter;
import com.creations.livebox.adapters.ObservableAdapter;
import com.creations.livebox.converters.Converter;
import com.creations.livebox.converters.ConvertersFactory;
import com.creations.livebox.datasources.Fetcher;
import com.creations.livebox.datasources.LocalDataSource;
import com.creations.livebox.datasources.disk.DiskLruDataSource;
import com.creations.livebox.datasources.disk.DiskPersistentDataSource;
import com.creations.livebox.rx.Transformers;
import com.creations.livebox.util.Logger;
import com.creations.livebox.util.Objects;
import com.creations.livebox.util.Optional;
import com.creations.livebox.validator.Validator;

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
public class Livebox<I, O> {

    private static final String TAG = "Livebox";

    // Keeps a record of in-flight requests.
    private static final ConcurrentHashMap<BoxKey, Observable> inFlightRequests = new ConcurrentHashMap<>();

    // A unique key that identifies this Livebox, used to keep track of in-flight requests.
    // Also this key is used to save and retrieve entries in cache
    private BoxKey mKey;

    // Indicates if we should make a fetch to remote data source even if the local data is still valid.
    private boolean mRefresh;

    // Indicates if we should ignore disk cache
    private boolean mIgnoreDiskCache;

    // Indicates if we should retry the remote data source request if an error occurs
    private boolean mRetryOnFailure;

    // Remote data source
    private Fetcher<I> mFetcher;

    // Local data sources
    private List<LocalDataSource<I, ?>> mLocalSources;

    // Stores validator for each store instance
    private Map<LocalDataSource<I, ?>, Validator> mValidators;

    // Keeps a mapping between a class types and a Converters.
    // Converter are used to convert the data read from data sources to the desired output.
    private Map<Class<?>, Converter<?, O>> mConvertersMap;

    // Converters factory, given a class type returns the converter instance to use.
    private Optional<ConvertersFactory<O>> mConverterFactory;

    // Keeps a log of request timestamps, used for age validator
    // After each request an entry will be added to journal with the timestamp of the request
    private Journal mJournal;

    // Transformer that adds share functionality to an observable
    private ObservableTransformer<O, O> withShare = new ObservableTransformer<O, O>() {
        @Override
        public ObservableSource<O> apply(Observable<O> upstream) {
            Logger.d(TAG, "Compose with share");
            Observable<O> observable = upstream
                    .doOnTerminate(() -> {
                        Logger.d(TAG, "Remove from inFlightRequests with key: " + mKey);
                        inFlightRequests.remove(mKey);
                    })
                    .share();

            inFlightRequests.putIfAbsent(mKey, observable);
            return observable;
        }
    };

    Livebox(BoxKey key, boolean refresh, boolean ignoreDiskCache, boolean retryOnFailure,
            Fetcher<I> fetcher, List<LocalDataSource<I, ?>> localSources,
            Map<LocalDataSource<I, ?>, Validator> validators,
            Map<Class<?>, Converter<?, O>> convertersMap,
            Optional<ConvertersFactory<O>> converterFactory) {

        this.mKey = key;
        this.mRefresh = refresh;
        this.mIgnoreDiskCache = ignoreDiskCache;
        this.mRetryOnFailure = retryOnFailure;
        this.mFetcher = fetcher;
        this.mLocalSources = localSources;
        this.mValidators = validators;
        this.mConvertersMap = convertersMap;
        this.mConverterFactory = converterFactory;
        //this.mJournal = Journal.create()
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
    private Observable<O> returnLocalData(Object localData) throws Exception {
        Logger.d(TAG, "returnLocalData() called with: localData = [" + localData + "]");
        return Observable.just(convert(localData));
    }

    // Fetch data from remote data source and pass new data to local sources.
    private Observable<O> fetchAndSave() {
        Logger.d(TAG, "fetchAndSave() called");
        return Observable
                .defer(mFetcher::fetch)
                .doOnNext(this::passFetchedDataToLocalSources)
                .compose(Transformers.withRetry(mRetryOnFailure))
                .map(this::convert);
    }

    @SuppressWarnings("unchecked")
    private <T> O convert(T data) throws Exception {

        Converter<T, O> converter;
        if (mConverterFactory.isPresent()) {
            Logger.d(TAG, "---> Using converter factory");
            converter = mConverterFactory.get().get((Class<T>) data.getClass());
        } else {
            converter = (Converter<T, O>) mConvertersMap.get(data.getClass());
        }

        if (Objects.nonNull(converter)) {
            Optional<O> convertedData = converter.convert(data);
            Logger.d(TAG, "---> Converter found for type: " + data.getClass());
            if (convertedData.isAbsent()) {
                throw new IllegalStateException("Converter: " + converter + "returned null for: " + data);
            }
            return convertedData.get();
        }

        // If no converter was found, we try casting because T type parameter
        // could have the same type as O type parameter, in that case no converter is needed.
        //noinspection unchecked
        return (O) data;
    }

    private void passFetchedDataToLocalSources(I data) {
        Logger.d(TAG, "\n");
        Logger.d(TAG, "Pass fresh data to local sources");
        for (LocalDataSource<I, ?> localSource : mLocalSources) {
            Logger.d(TAG, "---> Saving fresh data in: " + localSource);
            localSource.save(mKey.key(), data);

        }
    }

    private Observable<O> fetchFromRemoteDataSource() {
        return mFetcher.fetch()
                .map(this::convert)
                .compose(Transformers.withRetry(mRetryOnFailure))
                .compose(withShare);
    }

    @SuppressWarnings("WeakerAccess")
    public Observable<O> asObservable() {

        // Check if we have a request ongoing.
        // If we do return the Observable so the caller can subscribe to it.
        if (inFlightRequests.get(mKey) != null) {
            Logger.d(TAG, "---> We have a in-flight request for key: " + mKey);
            //noinspection unchecked
            return (Observable<O>) inFlightRequests.get(mKey);
        }

        // If ignore disk cache is true always hit remote data source
        if (mIgnoreDiskCache) {
            Logger.d(TAG, "Ignore disk cache, hit remote data source");
            return fetchFromRemoteDataSource();
        }

        // Get data from local source.
        Observable<O> retObservable = loadFromLocalSource()
                .flatMap((Function<Optional<?>, Observable<O>>) localResult -> {

                    // Local data is invalid, return a Observable that fetches remote data and
                    // saves to local data source.
                    if (localResult.isAbsent()) {
                        Logger.d(TAG, "Local data is invalid, hit remote data source and save");
                        return fetchAndSave();
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
                                fetchAndSave()
                        );
                    }
                });

        // Using share to avoid multiple requests to be executed.
        // #see https://stackoverflow.com/questions/35951942/single-observable-with-multiple-subscribers/35952390#35952390
        return retObservable.compose(withShare);
    }

    // Convenience method to return an Observable that observes on Android main thread
    // and subscribes on IO scheduler.
    public Observable<O> asAndroidObservable() {
        return asObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    // Convenience method that returns a live data instance.
    public LiveData<O> asLiveData() {
        return new LiveDataAdapter<O>().adapt(asObservable());
    }

    // Uses passed adapter to adapt the result observable.
    public <T> T as(@NonNull ObservableAdapter<O, T> adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("Adapter cannot be null");
        }
        return adapter.adapt(asObservable());
    }

    // A Key that uses a single string as identifier
    // Key must match the regex [a-z0-9_-]{1,120}.
    public final static class BoxKey {

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

        String key() {
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