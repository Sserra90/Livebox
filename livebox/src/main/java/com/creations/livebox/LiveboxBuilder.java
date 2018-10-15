package com.creations.livebox;

import android.support.annotation.NonNull;

import com.creations.livebox.Livebox.BoxKey;
import com.creations.livebox.adapters.ObservableAdapter;
import com.creations.livebox.converters.Converter;
import com.creations.livebox.converters.ConvertersFactory;
import com.creations.livebox.datasources.LocalDataSource;
import com.creations.livebox.datasources.factory.DataSourceFactory;
import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory;
import com.creations.livebox.datasources.fetcher.Fetcher;
import com.creations.livebox.util.Optional;
import com.creations.livebox.validator.AgeValidator;
import com.creations.livebox.validator.Validator;
import com.creations.livebox_common.util.Logger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static com.creations.livebox.util.Objects.isNull;

/**
 * @author Sérgio Serra on 29/08/2018.
 * Criations
 * <p>
 * <p>
 * Builds instances of {@link Livebox}
 * Usage ex:
 * <p>
 * {@code}
 * usersBox = new LiveboxBuilder<UsersRes, Users>()
 * .withKey("get_users")
 * .fetch(service::getUserList, UsersRes.class)
 * .addSource(Sources.MEMORY_LRU, memoryValidator)
 * .addSource(Sources.DISK_LRU, diskValidator)
 * .addConverter(UsersRes.class, usersRes -> Optional.of(Users.fromUsersRes(usersRes)))
 * .retryOnFailure()
 * .build();
 * {@code}
 * <p>
 * sergioserra99@gmail.com
 */
@SuppressWarnings({"UnusedReturnValue", "WeakerAccess"})
public class LiveboxBuilder<I, O> {

    public enum RetryStrategy {
        INTERVAL, BACKOFF
    }

    // Unique identifier for each livebox instance
    private BoxKey mKey;

    // Indicates if we should make a fetch to remote data source even if the local data is still valid.
    private boolean mRefresh = false;

    // Indicates if we should ignore cache
    private boolean mIgnoreCache = false;

    // Indicates if we should retry the remote data source request if an error occurs
    private boolean mRetryOnFailure = false;

    // Indicates the strategy to use when retrying defaults to INTERVAL
    private RetryStrategy mRetryStrategy = RetryStrategy.INTERVAL;

    // Indicates if an age validator was found
    private boolean mIsUsingAgeValidator = false;

    // Type that represents fetched data
    private Type mType;

    // Fetcher used to retrieve data from remote source
    private Fetcher<I> mFetcher;

    // Local data sources
    private List<LocalDataSource<I, ?>> mLocalSources = new ArrayList<>();

    // Stores validators for each data source
    private Map<LocalDataSource<I, ?>, Validator> mValidators = new HashMap<>();

    // Keeps a mapping between a class types and a Converters.
    // Converter are used to convert the data read from data sources to the desired output.
    private Map<Type, Converter<?, O>> mConvertersMap = new HashMap<>();

    // Converters factory, given a class type returns the converter instance to use.
    private Optional<ConvertersFactory<O>> mConverterFactory = Optional.empty();

    // Keeps a list of data sources factory.
    // We iterate the list and ask each DataSourceFactory instance for an instance of LocalDataSource
    private List<DataSourceFactory<I>> mDataSourceFactoryList = new ArrayList<>();

    public LiveboxBuilder<I, O> withKey(String key) {
        this.mKey = new BoxKey(key);
        return this;
    }

    public LiveboxBuilder<I, O> retryOnFailure() {
        return retryOnFailure(RetryStrategy.INTERVAL);
    }

    public LiveboxBuilder<I, O> retryOnFailure(RetryStrategy strategy) {
        mRetryOnFailure = true;
        mRetryStrategy = strategy;
        return this;
    }

    public LiveboxBuilder<I, O> ignoreCache(boolean ignoreCache) {
        this.mIgnoreCache = ignoreCache;
        return this;
    }

    public LiveboxBuilder<I, O> refresh(boolean refresh) {
        this.mRefresh = refresh;
        return this;
    }

    public LiveboxBuilder<I, O> fetch(@NonNull Fetcher<I> source, Type type) {
        requireNonNull(source, "Fetcher cannot be null");
        requireNonNull(type, "Type cannot be null");
        mType = type;
        mFetcher = source;
        mDataSourceFactoryList.add(new LiveboxDataSourceFactory<>(Livebox.getConfig().getSerializer(), type));
        return this;
    }

    public <T> LiveboxBuilder<I, O> addSource(@NonNull LocalDataSource<I, T> source, @NonNull Validator<T> validator) {
        requireNonNull(source, "Source cannot be null");
        requireNonNull(validator, "Validator cannot be null");

        if (validator instanceof AgeValidator) {
            mIsUsingAgeValidator = true;
        }

        mLocalSources.add(source);
        mValidators.put(source, validator);
        return this;
    }

    public <T> LiveboxBuilder<I, O> addSource(int dataSourceId, @NonNull Validator<T> validator) {
        for (DataSourceFactory<I> factory : mDataSourceFactoryList) {
            Optional<LocalDataSource<I, T>> localDataSource = factory.get(dataSourceId);
            if (localDataSource.isPresent()) {
                addSource(localDataSource.get(), validator);
                break;
            }
        }
        return this;
    }

    public LiveboxBuilder<I, O> addLocalSourceFactory(@NonNull DataSourceFactory<I> dataSourceFactory) {
        requireNonNull(dataSourceFactory, "Factory cannot be null");

        mDataSourceFactoryList.add(dataSourceFactory);
        return this;
    }

    public <T> LiveboxBuilder<I, O> addConverter(@NonNull Type type, @NonNull Converter<T, O> converter) {
        requireNonNull(converter, "Converter cannot be null");
        requireNonNull(type, "Type cannot be null");
        mConvertersMap.put(type, converter);
        return this;
    }

    public <T> LiveboxBuilder<I, O> addConverter(@NonNull Class<T> aClass, @NonNull Converter<T, O> converter) {
        requireNonNull(converter, "Converter cannot be null");
        requireNonNull(aClass, "Class cannot be null");
        mConvertersMap.put(aClass, converter);
        return this;
    }

    public LiveboxBuilder<I, O> addConverterFactory(ConvertersFactory<O> converterFactory) {
        requireNonNull(converterFactory, "Converter factory cannot be null");

        mConverterFactory = Optional.ofNullable(converterFactory);
        return this;
    }

    private void requireNonNull(Object o, String msg) {
        if (isNull(o)) {
            throw new IllegalArgumentException(msg);
        }
    }

    public Livebox<I, O> build() {
        return new Livebox<>(
                mKey, mType, mRefresh, mIgnoreCache, mRetryOnFailure, mRetryStrategy, mIsUsingAgeValidator,
                mFetcher, mLocalSources, mValidators, mConvertersMap,
                mConverterFactory
        );
    }

}
