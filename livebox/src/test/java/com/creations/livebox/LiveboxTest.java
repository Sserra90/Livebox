package com.creations.livebox;

import com.creations.livebox.config.Config;
import com.creations.livebox.converters.Converter;
import com.creations.livebox.datasources.LocalDataSource;
import com.creations.livebox.datasources.disk.DiskLruConfig;
import com.creations.livebox.datasources.disk.DiskPersistentConfig;
import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory.Sources;
import com.creations.livebox.datasources.fetcher.Fetcher;
import com.creations.livebox.util.FakeSource;
import com.creations.livebox.util.OnOffValidator;
import com.creations.livebox.validator.AgeValidator;
import com.creations.livebox.validator.Validator;
import com.creations.livebox_common.util.Bag;
import com.creations.livebox_common.util.Logger;
import com.creations.serializer_gson.LiveboxGsonSerializer;
import com.google.gson.reflect.TypeToken;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

import static com.creations.livebox.validator.AgeValidator.minutes;
import static com.creations.serializer_gson.UtilsKt.fromType;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Sérgio Serra
 * sergioserra99@gmail.com
 * <p>
 * Unit tests for {@link Livebox}
 */
public class LiveboxTest {

    @Rule
    public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

    private final static String TEST_KEY = "test_key";
    private final static File RES_FILE = new File("src/test/resources");
    private final static Type TYPE = fromType(new TypeToken<Bag<String>>() {
    });

    final static Config testConfig = new Config(
            new DiskLruConfig(RES_FILE, 10 * 1024 * 1024),
            new DiskPersistentConfig(RES_FILE),
            LiveboxGsonSerializer.create(),
            RES_FILE,
            true
    );

    @Before
    public void before() {
        Logger.disable();
    }

    private <T> void assertTestObserver(TestObserver<T> observer, T value) {
        observer
                .assertSubscribed()
                .assertNoErrors()
                .assertValue(value);
    }

    private Fetcher<Bag<String>> mockFetcher(Bag<String> bag) {
        final Fetcher bagFetcher = Mockito.mock(Fetcher.class);
        when(bagFetcher.fetch()).thenReturn(Observable.just(bag));
        //noinspection unchecked
        return bagFetcher;
    }

    private void fetcherCalled(Fetcher fetcher, int times) {
        verify(fetcher, times(times)).fetch();
    }

    /**
     * Check {@link Fetcher} instance was called two times after making two calls to Livebox when
     * {@link Livebox#mIgnoreDiskCache} is set to false.
     */
    @Test
    public void testFetchWithIgnoreCache() {
        Livebox.init(testConfig);

        final Bag<String> bag = new Bag<>("1", new ArrayList<>());
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        final Box<Bag<String>, Bag<String>> builder = new Box<>(TYPE);
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher)
                .ignoreCache(true)
                .build();

        final TestObserver<Bag<String>> bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe();
        bagBox.asObservable().subscribe(bagTestObserver);

        // Assert test observer
        assertTestObserver(bagTestObserver, bag);

        // Verify fetcher was called two times
        fetcherCalled(bagFetcher, 2);
    }

    /**
     * Check {@link Fetcher#fetch()} was called only one time when using a local source.
     */
    @Test
    public void testFetchWithSource() {
        Livebox.init(testConfig);

        // Setup mock fetcher
        final Bag<String> bag = new Bag<>("1", new ArrayList<>());
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        final Box<Bag<String>, Bag<String>> builder = new Box<>(TYPE);
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher)
                .addSource(FakeSource.create(), (Validator<Bag<String>>) (key, item) -> true)
                .ignoreCache(false)
                .build();

        final TestObserver<Bag<String>> bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe();
        bagBox.asObservable().subscribe(bagTestObserver);

        // Assert test observer
        assertTestObserver(bagTestObserver, bag);

        // Verify fetcher was called once
        fetcherCalled(bagFetcher, 1);
    }

    @Test
    public void testFetchWithSourceReadOnce() {
        Livebox.init(testConfig);

        // Setup mock fetcher
        final Bag<String> bag = new Bag<>("1", new ArrayList<>());
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        @SuppressWarnings("unchecked") final LocalDataSource<Bag<String>, Bag<String>> source = mock(LocalDataSource.class);

        final Box<Bag<String>, Bag<String>> builder = new Box<>(TYPE);
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher)
                .addSource(source, (Validator<Bag<String>>) (key, item) -> true)
                .ignoreCache(false)
                .build();

        final TestObserver<Bag<String>> bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe(bagTestObserver);

        // Assert test observer
        assertTestObserver(bagTestObserver, bag);

        // Verify fetcher was called once
        fetcherCalled(bagFetcher, 1);

        verify(source, times(1)).read(anyString());
    }

    /**
     * Fetch with an expired age validator, {@link Fetcher#fetch()} should be called two times.
     */
    @Test
    public void testFetchWithExpiredAgeValidator() {
        Livebox.init(testConfig);

        // Setup mock fetcher
        final Bag<String> bag = new Bag<>("1", new ArrayList<>());
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        final Box<Bag<String>, Bag<String>> builder = new Box<>(TYPE);
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher)
                .addSource(FakeSource.create(), minutes(0))
                .ignoreCache(false)
                .build();

        final TestObserver<Bag<String>> bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe();
        bagBox.asObservable().subscribe(bagTestObserver);

        // Assert test observer
        assertTestObserver(bagTestObserver, bag);

        // Verify fetcher was called two times
        fetcherCalled(bagFetcher, 2);
    }

    @Test
    public void testFetchWithAgeValidator() {
        Livebox.init(testConfig);

        // Setup mock fetcher
        final Bag<String> bag = new Bag<>("1", new ArrayList<>());
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        final Box<Bag<String>, Bag<String>> builder = new Box<>(TYPE);
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher)
                .addSource(FakeSource.create(), AgeValidator.create(TimeUnit.SECONDS.toMillis(10)))
                .ignoreCache(false)
                .build();

        final TestObserver<Bag<String>> bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe();
        bagBox.asObservable().subscribe(bagTestObserver);

        // Assert test observer
        assertTestObserver(bagTestObserver, bag);

        // Verify fetcher was called one time
        fetcherCalled(bagFetcher, 1);
    }

    /**
     * Add two local sources to Livebox.
     * Manually enable/disable data sources with custom {@link OnOffValidator}.
     * When the first local source is disabled, hit the second one.
     * When both are disable {@link Fetcher#fetch()} should be called.
     */
    @Test
    public void testFetchWithMultipleSources() {
        Livebox.init(testConfig);

        // Setup mock fetcher
        final Bag<String> bag = new Bag<>("1", new ArrayList<>());
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        final OnOffValidator<Bag<String>> fakeValidator = new OnOffValidator<>(true);
        final OnOffValidator<Bag<String>> diskValidator = new OnOffValidator<>(true);

        final Box<Bag<String>, Bag<String>> builder = new Box<>(TYPE);
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher)
                .addSource(FakeSource.create(), fakeValidator)
                .addSource(Sources.DISK_PERSISTENT, diskValidator)
                .ignoreCache(false)
                .build();

        TestObserver<Bag<String>> bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe();
        bagBox.asObservable().subscribe(bagTestObserver);

        // Assert test observer
        assertTestObserver(bagTestObserver, bag);

        // Verify fetcher was called one time
        fetcherCalled(bagFetcher, 1);

        // Disable memory data source
        fakeValidator.setOn(false);

        bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe(bagTestObserver);

        // Verify fetcher was not called one again, should hit disk source
        fetcherCalled(bagFetcher, 1);

        // Assert we received the correct value
        assertTestObserver(bagTestObserver, bag);

        // Disable disk source
        diskValidator.setOn(false);

        bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe(bagTestObserver);

        // Verify fetcher was called again
        fetcherCalled(bagFetcher, 2);

        // Assert we received the correct value
        assertTestObserver(bagTestObserver, bag);
    }

    /**
     * Check {@link Fetcher#fetch()} observable is retried when it emits an error.
     */
    @Test
    public void testFetchWithRetry() {
        Livebox.init(testConfig);

        final int[] nrInvocations = {0};
        final Fetcher<Bag<String>> bagFetcher = () -> Observable.fromCallable(() -> {
            nrInvocations[0]++;
            if (nrInvocations[0] == 1) {
                throw new RuntimeException();
            }
            return new Bag<String>("1", new ArrayList<>());
        });

        final Box<Bag<String>, Bag<String>> builder = new Box<>(TYPE);
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher)
                .ignoreCache(true)
                .retryOnFailure()
                .build();

        // Block until emits
        bagBox.asObservable().blockingFirst();

        // Check if number of invocations is two
        assertEquals(2, nrInvocations[0]);
    }

    @Test
    public void testFetchWithConverter() {
        Livebox.init(testConfig);

        // Setup mock fetcher
        final Bag<String> bag = new Bag<>("1", singletonList("1"));
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);


        final Box<Bag<String>, String> builder = new Box<>(TYPE);
        Livebox<Bag<String>, String> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher)
                .addConverter(TYPE, (Converter<Bag<String>, String>) Bag::getId)
                .ignoreCache(true)
                .build();

        final TestObserver<String> bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe(bagTestObserver);

        // Assert test observer
        assertTestObserver(bagTestObserver, "1");
    }

    /*@Test
    public void testLiveDataConverter() {
        Livebox.init(mDiskLruConfig);

        // Setup mock fetcher
        final Bag<String> bag = new Bag<>("1", singletonList("1"));
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        final Box<Bag<String>, Bag<String>> builder = new Box<>();
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher, TYPE)
                .ignoreCache(true)
                .build();

        bagBox.asLiveData().observeForever(bag1 -> assertEquals(bag, bag1));
    }*/

    /**
     * Make two requests for the same key.
     * The second one should subscribe to the first observable and wait for the first observable
     * to emit the result. {@link Fetcher#fetch()} should be called only one time.
     */
    @Test
    public void testMultipleRequestsWithShare() {
        Livebox.init(testConfig);

        // Setup fetcher
        final Bag<String> bag = new Bag<>("1", singletonList("1"));
        final int[] nrInvocations = {0};
        final Fetcher<Bag<String>> bagFetcher = () -> Observable.fromCallable(() -> {
            nrInvocations[0]++;
            return bag;
        });

        final Box<Bag<String>, Bag<String>> builder = new Box<>(TYPE);
        final Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher)
                .ignoreCache(true)
                .build();

        final TestObserver<Bag<String>> bagTestObserver = new TestObserver<>();

        bagBox.asObservable()
                .doOnSubscribe(disposable -> bagBox.asObservable().subscribe(bagTestObserver))
                .subscribe();

        // Assert we received the correct value
        assertTestObserver(bagTestObserver, bag);

        // Check number of invocations
        assertEquals(1, nrInvocations[0]);
    }

    @Test
    public void testCustomSourceWithMultipleConverters() {
        Livebox.init(testConfig);

        // Setup mock fetcher
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(new Bag<>("1", singletonList("1")));

        final Box<Bag<String>, String> builder = new Box<>(TYPE);
        Livebox<Bag<String>, String> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher)
                .addSource(new FakeSource<Bag<String>, Integer>() {
                    public Integer read(@NonNull String key) {
                        return 1;
                    }

                    @NonNull
                    public Type getType() {
                        return Integer.class;
                    }
                }, (Validator<Integer>) (key, item) -> true)
                .addConverter(Bag.class, Bag::getId)
                .addConverter(Integer.class, String::valueOf)
                .ignoreCache(false)
                .build();

        final TestObserver<String> bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe();
        bagBox.asObservable().subscribe(bagTestObserver);

        // Assert test observer
        assertTestObserver(bagTestObserver, "1");
    }

    @Test
    public void testReadFromLocalSourceAndRefresh() {
        Livebox.init(testConfig);

        // Setup mock fetcher
        final Bag<String> bag = new Bag<>("1", new ArrayList<>());
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        final Box<Bag<String>, Bag<String>> builder = new Box<>(TYPE);
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher)
                .addSource(FakeSource.create(), (Validator<Bag<String>>) (key, item) -> true)
                .ignoreCache(false)
                .refresh(true)
                .build();

        final TestObserver<Bag<String>> bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe();
        bagBox.asObservable().subscribe(bagTestObserver);

        //noinspection unchecked
        bagTestObserver
                .assertNoErrors()
                .assertValueCount(2)
                .assertValues(bag, bag);

        // Verify fetcher was called twice
        fetcherCalled(bagFetcher, 2);
    }

    @Test
    public void testInitWithNullConfig() {
        // Initialize livebox with null values.
        Livebox.init(
                new Config(
                        new DiskLruConfig(null, 10 * 1024 * 1024),
                        new DiskPersistentConfig(null),
                        LiveboxGsonSerializer.create(),
                        null,
                        false
                )
        );

        // Setup mock fetcher
        final Bag<String> bag = new Bag<>("1", new ArrayList<>());
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        final Box<Bag<String>, Bag<String>> builder = new Box<>(TYPE);
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher)
                .addSource(Sources.DISK_PERSISTENT, (Validator<Bag<String>>) (key, item) -> true)
                .ignoreCache(false)
                .build();

        final TestObserver<Bag<String>> bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe();
        bagBox.asObservable().subscribe(bagTestObserver);

        assertTestObserver(bagTestObserver, bag);
        fetcherCalled(bagFetcher, 2);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void tearDown() {
        if (RES_FILE.exists()) {
            File[] files = RES_FILE.listFiles((dir, name) -> name.startsWith(TEST_KEY));
            for (File file : files) {
                file.delete();
            }
        }
    }
}
