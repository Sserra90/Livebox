package com.creations.livebox;

import android.arch.core.executor.testing.InstantTaskExecutorRule;

import com.creations.livebox.config.Config;
import com.creations.livebox.datasources.disk.DiskPersistentDataSource;
import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory.Sources;
import com.creations.livebox.datasources.fetcher.Fetcher;
import com.creations.livebox.datasources.memory.InMemoryLruDataSource;
import com.creations.livebox.util.Bag;
import com.creations.livebox.util.FakeSource;
import com.creations.livebox.util.Logger;
import com.creations.livebox.util.OnOffValidator;
import com.creations.livebox.util.Optional;
import com.creations.livebox.validator.AgeValidator;
import com.creations.livebox.validator.Validator;
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

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Sérgio Serra on 05/09/2018.
 * Criations
 * sergioserra99@gmail.com
 * <p>
 * Unit tests for {@link Livebox}
 */
public class LiveboxTest {

    @Rule
    public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

    private final static String TEST_KEY = "test_key";
    private final static File RES_FILE = new File("src/test/resources");
    private final static Type TYPE = new TypeToken<Bag<String>>() {
    }.getType();

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
        @SuppressWarnings("unchecked") final Fetcher<Bag<String>> bagFetcher = (Fetcher<Bag<String>>) Mockito.mock(Fetcher.class);
        when(bagFetcher.fetch()).thenReturn(Observable.just(bag));
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
        Livebox.init(new Config());

        final Bag<String> bag = new Bag<>("1", new ArrayList<>());
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        final LiveboxBuilder<Bag<String>, Bag<String>> builder = new LiveboxBuilder<>();
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher, TYPE)
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
        Livebox.init(new Config());

        // Setup mock fetcher
        final Bag<String> bag = new Bag<>("1", new ArrayList<>());
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        final LiveboxBuilder<Bag<String>, Bag<String>> builder = new LiveboxBuilder<>();
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher, TYPE)
                .addSource(Sources.MEMORY_LRU, (Validator<Bag<String>>) (key, item) -> true)
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

    /**
     * Fetch with an expired age validator, {@link Fetcher#fetch()} should be called two times.
     */
    @Test
    public void testFetchWithExpiredAgeValidator() {
        Livebox.init(new Config().journalDir(RES_FILE));

        // Setup mock fetcher
        final Bag<String> bag = new Bag<>("1", new ArrayList<>());
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        final LiveboxBuilder<Bag<String>, Bag<String>> builder = new LiveboxBuilder<>();
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher, TYPE)
                .addSource(Sources.MEMORY_LRU, AgeValidator.create(TimeUnit.SECONDS.toMillis(0)))
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
        Livebox.init(new Config().journalDir(RES_FILE));

        // Setup mock fetcher
        final Bag<String> bag = new Bag<>("1", new ArrayList<>());
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        final LiveboxBuilder<Bag<String>, Bag<String>> builder = new LiveboxBuilder<>();
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher, TYPE)
                .addSource(Sources.MEMORY_LRU, AgeValidator.create(TimeUnit.SECONDS.toMillis(5)))
                .ignoreCache(false)
                .build();

        final TestObserver<Bag<String>> bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe();
        bagBox.asObservable().subscribe(bagTestObserver);

        // Assert test observer
        assertTestObserver(bagTestObserver, bag);

        // Verify fetcher was called two times
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
        Livebox.init(new Config()
                .persistentCacheConfig(new DiskPersistentDataSource.Config(RES_FILE))
                .journalDir(RES_FILE)
        );

        // Setup mock fetcher
        final Bag<String> bag = new Bag<>("1", new ArrayList<>());
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        final OnOffValidator<Bag<String>> memoryValidator = new OnOffValidator<>(true);
        final OnOffValidator<Bag<String>> diskValidator = new OnOffValidator<>(true);

        final LiveboxBuilder<Bag<String>, Bag<String>> builder = new LiveboxBuilder<>();
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher, TYPE)
                .addSource(Sources.MEMORY_LRU, memoryValidator)
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
        memoryValidator.setOn(false);

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
        Livebox.init(new Config());

        final int[] nrInvocations = {0};
        final Fetcher<Bag<String>> bagFetcher = () -> Observable.fromCallable(() -> {
            nrInvocations[0]++;
            if (nrInvocations[0] == 1) {
                throw new RuntimeException();
            }
            return new Bag<String>("1", new ArrayList<>());
        });

        final LiveboxBuilder<Bag<String>, Bag<String>> builder = new LiveboxBuilder<>();
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher, TYPE)
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
        Livebox.init(new Config());

        // Setup mock fetcher
        final Bag<String> bag = new Bag<>("1", singletonList("1"));
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        final LiveboxBuilder<Bag<String>, String> builder = new LiveboxBuilder<>();
        Livebox<Bag<String>, String> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher, TYPE)
                .addConverter(Bag.class, o -> Optional.of(o.getId()))
                .ignoreCache(true)
                .build();

        final TestObserver<String> bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe(bagTestObserver);

        // Assert test observer
        assertTestObserver(bagTestObserver, "1");
    }

    @Test
    public void testLiveDataConverter() {
        Livebox.init(new Config());

        // Setup mock fetcher
        final Bag<String> bag = new Bag<>("1", singletonList("1"));
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(bag);

        final LiveboxBuilder<Bag<String>, Bag<String>> builder = new LiveboxBuilder<>();
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher, TYPE)
                .ignoreCache(true)
                .build();

        bagBox.asLiveData().observeForever(bag1 -> assertEquals(bag, bag1));
    }

    /**
     * Make two requests for the same key.
     * The second one should subscribe to the first observable and wait for the first observable
     * to emit the result. {@link Fetcher#fetch()} should be called only one time.
     */
    @Test
    public void testMultipleRequestsWithShare() {
        Livebox.init(new Config());

        // Setup fetcher
        final Bag<String> bag = new Bag<>("1", singletonList("1"));
        final int[] nrInvocations = {0};
        final Fetcher<Bag<String>> bagFetcher = () -> Observable.fromCallable(() -> {
            nrInvocations[0]++;
            return bag;
        });

        final LiveboxBuilder<Bag<String>, Bag<String>> builder = new LiveboxBuilder<>();
        final Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher, TYPE)
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

        Livebox.init(new Config());

        // Setup mock fetcher
        final Fetcher<Bag<String>> bagFetcher = mockFetcher(new Bag<>("1", singletonList("1")));

        final LiveboxBuilder<Bag<String>, String> builder = new LiveboxBuilder<>();
        Livebox<Bag<String>, String> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher, TYPE)
                .addSource(new FakeSource<Bag<String>, Integer>() {
                    public Optional<Integer> read(String key) {
                        return Optional.of(1);
                    }
                }, (key, item) -> true)
                .addConverter(Bag.class, o -> Optional.of(o.getId()))
                .addConverter(Integer.class, o -> Optional.of(String.valueOf(o)))
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
        Livebox.init(new Config());


    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void tearDown() {
        InMemoryLruDataSource.create().clear(TEST_KEY);
        if (RES_FILE.exists()) {
            File[] files = RES_FILE.listFiles((dir, name) -> name.startsWith(TEST_KEY));
            for (File file : files) {
                file.delete();
            }
        }
    }
}
