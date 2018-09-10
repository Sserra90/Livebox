package com.creations.livebox;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import com.creations.livebox.LiveboxBuilder.RetryStrategy;
import com.creations.livebox.adapters.AutoDisposeAdapter;
import com.creations.livebox.adapters.LiveDataAdapter;
import com.creations.livebox.adapters.ObservableAdapter;
import com.creations.livebox.config.Config;
import com.creations.livebox.converters.Converter;
import com.creations.livebox.converters.ConvertersFactory;
import com.creations.livebox.datasources.LocalDataSource;
import com.creations.livebox.datasources.disk.DiskLruDataSource;
import com.creations.livebox.datasources.disk.DiskPersistentDataSource;
import com.creations.livebox.datasources.fetcher.Fetcher;
import com.creations.livebox.rx.Transformers;
import com.creations.livebox.serializers.LiveboxJacksonSerializer;
import com.creations.livebox.util.Logger;
import com.creations.livebox.util.Optional;
import com.creations.livebox.util.io.Utils;
import com.creations.livebox.validator.Journal;
import com.creations.livebox.validator.Validator;
import com.uber.autodispose.ObservableSubscribeProxy;
import com.uber.autodispose.lifecycle.LifecycleScopeProvider;

import java.io.File;
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
import io.reactivex.schedulers.Schedulers;

import static com.creations.livebox.util.Objects.nonNull;

/**
 * @author Sérgio Serra
 * Criations
 * sergioserra99@gmail.com
 */
public class Livebox<I, O> {

    private static final String TAG = "Livebox";

    private static final String LRU_DISK_CACHE_DIR = "livebox_disk_lru_cache";
    private static final String PERSISTENT_DISK_CACHE_DIR = "livebox_disk_persistent_cache";
    private static final String JOURNAL_DIR = "livebox_journal_dir";

    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 100; // 100MB
    private static final int DEFAULT_DISK_CACHE_SIZE_PERCENT = 10; // 10% of free disk space

    public static void init(Context context) {
        final File lruCacheDir = Utils.getCacheDirectory(context, LRU_DISK_CACHE_DIR);
        final long lurCacheSize = Utils.getCacheSizeInBytes(
                lruCacheDir,
                DEFAULT_DISK_CACHE_SIZE_PERCENT / 100F,
                DEFAULT_DISK_CACHE_SIZE
        );
        final File persistentCacheDir = Utils.getCacheDirectory(context, PERSISTENT_DISK_CACHE_DIR);

        init(new Config()
                .journalDir(Utils.getCacheDirectory(context, JOURNAL_DIR))
                .lruCacheConfig(new DiskLruDataSource.Config(lruCacheDir, lurCacheSize))
                .persistentCacheConfig(new DiskPersistentDataSource.Config(persistentCacheDir))
                .addSerializer(LiveboxJacksonSerializer.create())
        );
    }

    @SuppressWarnings("WeakerAccess")
    public static void init(Config config) {
        mInit = true;
        mConfig = config;

        DiskPersistentDataSource.setConfig(getConfig().getPersistentConfig());
        DiskLruDataSource.setConfig(getConfig().getLruConfig());

        if (nonNull(config.getJournalDir())) {
            journal = Journal.create(config.getJournalDir());
        }
    }

    // Keeps a record of in-flight requests.
    private static final ConcurrentHashMap<BoxKey, Observable> inFlightRequests = new ConcurrentHashMap<>();

    // Journal that keeps a log of requests timestamps
    public static Journal journal;

    private static Config mConfig;

    // Indicates if Livebox was initialized
    private static boolean mInit = false;

    // A unique key that identifies this Livebox, used to keep track of in-flight requests.
    // Also this key is used to save and retrieve entries in cache
    private BoxKey mKey;

    // Indicates if we should make a fetch to remote data source even if the local data is still valid.
    private boolean mRefresh;

    // Indicates if we should ignore disk cache
    private boolean mIgnoreDiskCache;

    // Indicates if we should retry the remote data source request if an error occurs
    private boolean mRetryOnFailure;

    // Indicates the strategy to use when retrying defaults to INTERVAL
    private RetryStrategy mRetryStrategy;

    // If an age validator was found
    private boolean mIsUsingAgeValidator;

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

    // Transformer that adds share functionality to an observable
    private ObservableTransformer<O, O> withShare = new ObservableTransformer<O, O>() {
        @Override
        public ObservableSource<O> apply(Observable<O> upstream) {
            Logger.d(TAG, "Compose with share");
            Observable<O> observable = upstream
                    .doOnComplete(() -> {
                        Logger.d(TAG, "Remove from inFlightRequests with key: " + mKey);
                        inFlightRequests.remove(mKey);
                    })
                    .share();

            inFlightRequests.putIfAbsent(mKey, observable);
            return observable;
        }
    };

    Livebox(BoxKey key, boolean refresh, boolean ignoreDiskCache, boolean retryOnFailure, RetryStrategy retryStrategy,
            boolean isUsingAgeValidator, Fetcher<I> fetcher, List<LocalDataSource<I, ?>> localSources,
            Map<LocalDataSource<I, ?>, Validator> validators,
            Map<Class<?>, Converter<?, O>> convertersMap,
            Optional<ConvertersFactory<O>> converterFactory) {

        if (!mInit) {
            throw new IllegalStateException("You must call Livebox.init() before creating any instance");
        }

        mKey = key;
        mRefresh = refresh;
        mIgnoreDiskCache = ignoreDiskCache;
        mRetryOnFailure = retryOnFailure;
        mRetryStrategy = retryStrategy;
        mIsUsingAgeValidator = isUsingAgeValidator;
        mFetcher = fetcher;
        mLocalSources = localSources;
        mValidators = validators;
        mConvertersMap = convertersMap;
        mConverterFactory = converterFactory;
    }

    /**
     * Reads data from local sources.
     * <p>
     * Iterates {@link #mLocalSources} list and for each {@link LocalDataSource} tries to read
     * local data indexed by {@link #mKey}. If data is present, calls {@link Validator} to check
     * if is still valid, if it is use it. Otherwise if no valid local data is found return an
     * empty {@link Optional#empty()}.
     *
     * @return an Observable that will emit an {@link Optional} that may or may not contain data.
     */
    private Observable<Optional<?>> readFromLocalSources() {
        Logger.d(TAG, "readFromLocalSources() called");

        if (mLocalSources.isEmpty()) {
            throw new IllegalStateException("No local sources found");
        }

        for (LocalDataSource<I, ?> source : mLocalSources) {
            Logger.d(TAG, "---> Hit source " + source);

            final Optional<?> data = source.read(mKey.key());
            if (data.isAbsent()) {
                continue;
            }

            @SuppressWarnings("unchecked")
            boolean isValid = mValidators.get(source).validate(mKey.key(), data.get());
            if (!isValid) {
                Logger.d(TAG, "---> Data from source " + source + " is not valid. Clear it");
                source.clear(mKey.key());
                continue;
            }

            Logger.d(TAG, "---> Data from source " + source + " is valid");
            return Observable.fromCallable(() -> data);
        }

        Logger.d(TAG, "---> No valid data found");
        return Observable.fromCallable(Optional::empty);

    }

    // Maps data from local data source type -> output type
    private Observable<O> returnLocalData(Object localData) throws Exception {
        Logger.d(TAG, "returnLocalData() called with: localData = [" + localData + "]");
        return Observable.just(convert(localData));
    }

    /**
     * Fetch data using {@link #mFetcher}.
     *
     * @param saveToLocalSources determines if data must be saved to local sources.
     * @return a defer Observable that will emit items when subscribed
     */
    private Observable<O> fetch(boolean saveToLocalSources) {
        Observable<I> obs = Observable.defer(mFetcher::fetch);

        if (saveToLocalSources) {
            obs = obs.doOnNext(this::passFetchedDataToLocalSources);
        }

        return obs.map(this::convert)
                .compose(Transformers.withRetry(mRetryOnFailure, mRetryStrategy))
                .compose(withShare);
    }

    /**
     * Passes data fetched when calling {@link #fetch(boolean)} to local sources.
     *
     * @param data the data received from {{@link #mFetcher}}
     */
    private void passFetchedDataToLocalSources(I data) {
        if (mIsUsingAgeValidator) {
            Logger.d(TAG, "---> Save in journal for key: " + mKey);
            journal.save(mKey.key(), System.currentTimeMillis());
        }

        Logger.d(TAG, "Pass fresh data to local sources");
        for (LocalDataSource<I, ?> localSource : mLocalSources) {
            Logger.d(TAG, "---> Saving fresh data in: " + localSource);
            localSource.save(mKey.key(), data);
        }
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

        if (nonNull(converter)) {
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
            return fetch(false);
        }

        // Get data from local source.
        Observable<O> retObservable = readFromLocalSources()
                .flatMap((Function<Optional<?>, Observable<O>>) localResult -> {

                    // Local data is invalid, return a Observable that fetches remote data and
                    // saves to local data source.
                    if (localResult.isAbsent()) {
                        Logger.d(TAG, "Local data is invalid, hit remote data source and save");
                        return fetch(true);
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
                                fetch(true)
                        );
                    }
                });

        // Using share to avoid multiple requests to be executed.
        // #see https://stackoverflow.com/questions/35951942/single-observable-with-multiple-subscribers/35952390#35952390
        return retObservable.compose(withShare);
    }

    /**
     * Convenience method to return an Observable that observes on Android main thread
     * and subscribes on IO scheduler.
     *
     * @return an Observable that will emit on {@link Schedulers#io()}
     * and observer in {@link AndroidSchedulers#mainThread()}
     */
    public Observable<O> asAndroidObservable() {
        return asObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Convenience method that returns a live data instance.
     *
     * @return {@link LiveData} instance
     */
    public LiveData<O> asLiveData() {
        return new LiveDataAdapter<O>().adapt(asObservable());
    }

    /**
     * Scopes the Observable within {@param LifecycleScopeProvider}
     *
     * @param scopeProvider the scope provider
     * @return ObservableSubscribeProxy
     */
    public ObservableSubscribeProxy<O> scoped(LifecycleScopeProvider scopeProvider) {
        return AutoDisposeAdapter.<O>android(scopeProvider).adapt(asObservable());
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

    public static Config getConfig() {
        return mConfig;
    }
}