package com.creations.livebox;

import com.creations.livebox.config.Config;
import com.creations.livebox.datasources.Fetcher;
import com.creations.livebox.datasources.disk.DiskPersistentDataSource;
import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory.Sources;
import com.creations.livebox.util.Logger;
import com.creations.livebox.validator.AgeValidator;
import com.creations.livebox.validator.Validator;
import com.google.gson.reflect.TypeToken;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Sérgio Serra on 05/09/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class LiveboxTest {

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
    public void fetchWithIgnoreCache() {
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
    public void fetchWithSource() {
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
    public void fetchWithExpiredAgeValidator() {
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
    public void fetchWithAgeValidator() {
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

    @Test
    public void fetchWithMultipleSources() {
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
