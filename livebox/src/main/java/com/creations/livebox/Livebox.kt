package com.creations.livebox

import com.creations.livebox.Box.RetryStrategy
import com.creations.livebox.adapters.AndroidAdapter
import com.creations.livebox.config.Config
import com.creations.livebox.converters.Converter
import com.creations.livebox.datasources.LocalDataSource
import com.creations.livebox.datasources.disk.DiskLruDataSource
import com.creations.livebox.datasources.disk.DiskPersistentDataSource
import com.creations.livebox.datasources.fetcher.Fetcher
import com.creations.livebox.rx.Transformers
import com.creations.livebox.util.Optional
import com.creations.livebox.util.isNull
import com.creations.livebox.validator.Journal
import com.creations.livebox.validator.Validator
import com.creations.livebox_common.adapter.ObservableAdapter
import com.creations.livebox_common.util.Logger
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.annotations.NonNull
import io.reactivex.schedulers.Schedulers
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * @author Sérgio Serra
 * sergioserra99@gmail.com
 */
class Livebox<I, O> internal constructor(
        // A unique key that identifies this Livebox, used to keep track of in-flight requests.
        // Also this key is used to save and retrieve entries in cache
        private val mKey: BoxKey,
        // Type that represents fetched data
        private val mType: Type,
        // Indicates if we should make a fetch to remote data source even if the local data is still valid.
        private val mRefresh: Boolean,
        // Indicates if we should ignore disk cache
        private val mIgnoreDiskCache: Boolean,
        // Indicates if we should retry the remote data source request if an error occurs
        private val mRetryOnFailure: Boolean,
        // Indicates the strategy to use when retrying defaults to INTERVAL
        private val mRetryStrategy: RetryStrategy,
        // If an age validator was found
        private val mIsUsingAgeValidator: Boolean,
        // Remote data source
        private val mFetcher: Fetcher<I>,
        // Local data sources
        private val mLocalSources: List<LocalDataSource<I, *>>,
        // Stores validator for each store instance
        private val mValidators: Map<LocalDataSource<I, *>, Validator<*>>,
        // Keeps a mapping between a types and converter.
        // Converter are used to convert the data read from data sources to the desired output.
        private val mConvertersMap: Map<Type, Converter<*, O>>
) {

    // Transformer that adds share functionality to an observable
    private val withShare = ObservableTransformer<O, O> { upstream ->
        Logger.d(TAG, "Compose with share")
        val observable = upstream
                .doOnComplete {
                    Logger.d(TAG, "Remove from inFlightRequests with key %s", mKey)
                    inFlightRequests.remove(mKey)
                }
                .share()

        inFlightRequests.putIfAbsent(mKey, observable)
        observable
    }

    init {
        if (!mInit) {
            throw IllegalStateException("You must call Livebox.init() before creating any instance")
        }
    }

    /**
     * Reads data from local sources.
     *
     * Iterates [mLocalSources] list and for each [LocalDataSource] tries to read
     * local data indexed by [mKey]. If data is present, calls [Validator] to check
     * if is still valid, if it is use it. Otherwise if no valid local data is found return an
     * empty [Optional.empty].
     *
     * @return an Observable that will emit an [Optional] that may or may not contain data.
     */
    private fun readFromLocalSources(): Optional<Payload<Any>>? {
        Logger.d(TAG, "Try to read from local data sources")

        for (source in mLocalSources) {
            Logger.d(TAG, "Hit source %s", source)

            // If data is null continue
            val data = source.read(mKey.key) ?: continue

            @Suppress("UNCHECKED_CAST")
            val validator: Validator<Any> = mValidators[source] as Validator<Any>? ?: continue

            // Validate data
            if (!validator.validate(mKey.key, data)) {
                Logger.d(TAG, "Data from source %s is not valid. Clear it", source)
                source.clear(mKey.key)
                continue
            }

            Logger.d(TAG, "---> Data from source $source is valid")
            return Optional.of(Payload(source.type, data))
        }

        Logger.d(TAG, "---> No valid data found")
        return Optional.empty()
    }

    @Throws(Exception::class)
    private fun returnLocalData(localData: Any, type: Type): Observable<O> {
        Logger.d(TAG, "Return local data: %s", localData)
        return Observable.just(convert(localData, type))
    }

    /**
     * Fetch data using [mFetcher].
     *
     * @param saveToLocalSources determines if data must be saved to local sources.
     * @return a defer Observable that will emit items when subscribed
     */
    private fun fetch(saveToLocalSources: Boolean): Observable<O> {
        var obs = Observable.defer<I> { mFetcher.fetch() }

        if (saveToLocalSources) obs = obs.doOnNext { passFetchedDataToLocalSources(it) }

        return obs
                .map { i -> convert(i, mType) }
                .compose(Transformers.withRetry(mRetryOnFailure, mRetryStrategy))
    }

    /**
     * Passes data fetched when calling [fetch] to local sources.
     * @param data the data received from {[mFetcher]}
     */
    private fun passFetchedDataToLocalSources(data: I) {
        if (mIsUsingAgeValidator) {
            Logger.d(TAG, "Save in journal for key: $mKey")
            journal?.save(mKey.key, System.currentTimeMillis())
        }

        Logger.d(TAG, "Pass fresh data to local sources")
        for (localSource in mLocalSources) {
            Logger.d(TAG, "Saving fresh data in: $localSource")
            localSource.save(mKey.key, data)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(Exception::class)
    private fun <T> convert(data: T, type: Type): O {

        if (mConvertersMap.containsKey(type)) {
            val converter: Converter<T, O> = mConvertersMap[type] as Converter<T, O>
            val convertedData = converter.convert(data)
            Logger.d(TAG, "Converter found for type: $type")
            if (isNull(convertedData)) {
                throw IllegalStateException("Converter: " + converter + "returned null for: " + data)
            }
            return convertedData
        }

        // If no converter was found, we try casting because T type parameter
        // could have the same type as O type parameter, in that case no converter is needed.
        return data as O
    }

    fun asObservable(): Observable<O> {
        Logger.d(TAG, "Start request for key: %s", mKey)

        // Check if we have a request ongoing.
        // If we do return the Observable so the caller can subscribe to it.
        if (inFlightRequests[mKey] != null) {
            Logger.d(TAG, "We have a in-flight request for key: %s", mKey)
            @Suppress("UNCHECKED_CAST")
            return inFlightRequests[mKey] as Observable<O>
        }

        // If ignore disk cache is true always hit remote data source
        if (mIgnoreDiskCache) {
            Logger.d(TAG, "Ignore disk cache, hit remote data source")
            return fetch(false).compose(withShare)
        }

        // Get data from local source.
        val retObservable = Observable
                .fromCallable<Optional<Payload<Any>>> { readFromLocalSources() }
                .flatMap { payload: Optional<Payload<Any>> ->

                    // Local data is invalid, return an Observable that fetches remote data and
                    // saves to local data sources.
                    if (payload.isAbsent) {
                        Logger.d(TAG, "Local data is invalid, hit remote data source and save")
                        return@flatMap fetch(true)
                    }

                    // At this point we know we have valid local data,
                    // if the user does not want to refresh return, otherwise return a Observable
                    // that emits local data, fetches the latest data from remote source and saves it.
                    return@flatMap if (!mRefresh) {
                        Logger.d(TAG, "Local data is valid, do not hit remote data source")
                        returnLocalData(payload.get().data, payload.get().type)
                    } else {
                        Logger.d(TAG, "Local data is valid but still hit remote data source to refresh data")
                        Observable.concat<O>(
                                returnLocalData(payload.get().data, payload.get().type),
                                fetch(true)
                        )
                    }

                }

        // Using share to avoid multiple requests to be executed.
        // #see https://stackoverflow.com/questions/35951942/single-observable-with-multiple-subscribers/35952390#35952390
        return retObservable.compose(withShare)
    }

    /**
     * Convenience method to return an Observable that observes on Android main thread
     * and subscribes on IO scheduler.
     *
     * @return an Observable that will emit on [Schedulers.io]
     * and observer in [AndroidSchedulers.mainThread]
     */
    fun asAndroidObservable(): Observable<O> = `as`(AndroidAdapter())

    // Uses passed adapter to adapt the result observable.
    fun <T> `as`(@NonNull adapter: ObservableAdapter<O, T>?): T {
        if (adapter == null) {
            throw IllegalArgumentException("Adapter cannot be null")
        }
        return adapter.adapt(asObservable())
    }

    companion object {
        const val TAG = "Livebox"

        // Keeps a record of in-flight requests.
        private val inFlightRequests = ConcurrentHashMap<BoxKey, Observable<*>>()

        // Journal that keeps a log of requests timestamps
        var journal: Journal? = null
            get() {
                if (!mInit) {
                    throw IllegalStateException("Livebox.init() was not called")
                }
                return field
            }

        lateinit var config: Config

        private var mInit = false

        @JvmStatic
        fun init(liveboxConfig: Config) {
            mInit = true
            config = liveboxConfig

            Logger.d(TAG, "Init with config: $config")

            if (config.isLoggingDisabled) {
                Logger.disable()
            }

            if (isNull(config.serializer)) {
                throw IllegalArgumentException("Serializer cannot be null")
            }

            DiskPersistentDataSource.config = config.persistentConfig
            DiskLruDataSource.config = config.diskLruConfig

            if (config.journalDir != null) {
                journal = Journal.create(config.journalDir)
            }
        }
    }

}

private data class Payload<T>(val type: Type, val data: T)

// A Key that uses a single string as identifier
// Key must match the regex [a-z0-9_-]{1,120}.
data class BoxKey internal constructor(val key: String) {

    init {
        validateKey(key)
    }

    private fun validateKey(key: String): String {
        val matcher = LEGAL_KEY_PATTERN.matcher(key)
        if (!matcher.matches()) {
            throw IllegalArgumentException(
                    "keys must match regex $STRING_KEY_PATTERN: \"$key\"")
        }
        return key
    }

    companion object {
        private const val STRING_KEY_PATTERN = "[a-z0-9_-]{1,120}"
        private val LEGAL_KEY_PATTERN = Pattern.compile(STRING_KEY_PATTERN)
    }
}
