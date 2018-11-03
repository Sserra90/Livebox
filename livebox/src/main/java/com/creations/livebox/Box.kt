package com.creations.livebox

import com.creations.livebox.converters.Converter
import com.creations.livebox.datasources.LocalDataSource
import com.creations.livebox.datasources.factory.DataSourceFactory
import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory
import com.creations.livebox.datasources.fetcher.Fetcher
import com.creations.livebox.validator.AgeValidator
import com.creations.livebox.validator.Validator
import io.reactivex.Observable
import io.reactivex.annotations.NonNull
import java.lang.reflect.Type
import java.util.*

/**
 * @author Sérgio Serra
 * Builds instances of [Livebox]
 */
class Box<I, O>(private val mType: Type) {

    // Unique identifier for each livebox instance
    private lateinit var mKey: BoxKey

    // Indicates if we should make a fetch to remote data source even if the local data is still valid.
    private var mRefresh = false

    // Indicates if we should ignore cache
    private var mIgnoreCache = false

    // Indicates if we should retry the remote data source request if an error occurs
    private var mRetryOnFailure = false

    // Indicates the strategy to use when retrying defaults to INTERVAL
    private var mRetryStrategy = RetryStrategy.INTERVAL

    // Indicates if an age validator was found
    private var mIsUsingAgeValidator = false

    // Fetcher used to retrieve data from remote source
    private lateinit var mFetcher: Fetcher<I>

    // Local data sources
    private val mLocalSources = ArrayList<LocalDataSource<I, *>>()

    // Stores validators for each data source
    private val mValidators = HashMap<LocalDataSource<I, *>, Validator<*>>()

    // Keeps a mapping between a class types and a Converters.
    // Converter are used to convert the data read from data sources to the desired output.
    private val mConvertersMap = HashMap<Type, Converter<*, O>>()

    // Keeps a list of data sources factory.
    // We iterate the list and ask each DataSourceFactory instance for an instance of LocalDataSource
    private val mDataSourceFactoryList = ArrayList<DataSourceFactory<I>>()

    init {
        mDataSourceFactoryList.add(LiveboxDataSourceFactory(Livebox.config.serializer, mType))
    }

    enum class RetryStrategy {
        INTERVAL, BACKOFF
    }

    fun withKey(key: String): Box<I, O> = apply {
        mKey = BoxKey(key)
    }

    @JvmOverloads
    fun retryOnFailure(strategy: RetryStrategy = RetryStrategy.INTERVAL): Box<I, O> = apply {
        mRetryOnFailure = true
        mRetryStrategy = strategy
    }

    fun ignoreCache(ignoreCache: Boolean): Box<I, O> = apply {
        mIgnoreCache = ignoreCache
    }

    fun refresh(refresh: Boolean): Box<I, O> = apply {
        mRefresh = refresh
    }

    fun fetch(@NonNull source: () -> Observable<I>): Box<I, O> = apply {
        fetch(object : Fetcher<I> {
            override fun fetch(): Observable<I> = source()
        })
    }

    fun fetch(@NonNull source: Fetcher<I>): Box<I, O> = apply {
        mFetcher = source
    }

    fun <T> addSource(@NonNull source: LocalDataSource<I, T>, @NonNull validator: (key: String, T) -> Boolean): Box<I, O> = apply {
        addSource(source, object : Validator<T> {
            override fun validate(key: String, item: T): Boolean = validator(key, item)
        })
    }

    fun <T> addSource(@NonNull source: LocalDataSource<I, T>, @NonNull validator: Validator<T>): Box<I, O> = apply {
        if (validator is AgeValidator<*>) {
            mIsUsingAgeValidator = true
        }

        mLocalSources.add(source)
        mValidators[source] = validator
    }

    fun addSource(dataSourceId: Int, @NonNull validator: (key: String, I) -> Boolean): Box<I, O> = apply {
        addSource(dataSourceId, object : Validator<I> {
            override fun validate(key: String, item: I): Boolean = validator(key, item)
        })
    }

    fun <T> addSource(dataSourceId: Int, @NonNull validator: Validator<T>): Box<I, O> = apply {
        mDataSourceFactoryList.forEach {
            val localDataSource = it.get<T>(dataSourceId)
            if (localDataSource != null) {
                addSource(localDataSource, validator)
                return@forEach
            }
        }
    }

    fun <T> addConverter(@NonNull converter: (T) -> O): Box<I, O> = apply {
        addConverter(mType, object : Converter<T, O> {
            override fun convert(o: T): O = converter(o)
        })
    }

    fun <T> addConverter(@NonNull type: Type, @NonNull converter: Converter<T, O>): Box<I, O> = apply {
        mConvertersMap[type] = converter
    }

    fun <T> addConverter(@NonNull aClass: Class<T>, @NonNull converter: Converter<T, O>): Box<I, O> = apply {
        mConvertersMap[aClass] = converter
    }

    fun build(): Livebox<I, O> = Livebox(
            mKey, mType, mRefresh, mIgnoreCache, mRetryOnFailure, mRetryStrategy,
            mIsUsingAgeValidator, mFetcher, mLocalSources, mValidators, mConvertersMap
    )

}

