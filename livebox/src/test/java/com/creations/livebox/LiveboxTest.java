package com.creations.livebox;

import com.creations.livebox.config.Config;
import com.creations.livebox.datasources.Fetcher;
import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory.Sources;
import com.creations.livebox.util.Logger;
import com.creations.livebox.validator.Validator;
import com.google.gson.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Type;
import java.util.ArrayList;

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

    @Before
    public void before() {
        Logger.disable();
    }

    /**
     * Check {@link Fetcher} instance was called two times after making two calls to Livebox when
     * {@link Livebox#mIgnoreDiskCache} is set to false.
     */
    @Test
    public void fetchWithIgnoreCache() {
        Livebox.init(new Config());

        // Setup mock fetcher
        @SuppressWarnings("unchecked") final Fetcher<Bag<String>> bagFetcher = (Fetcher<Bag<String>>) Mockito.mock(Fetcher.class);
        final Bag<String> bag = new Bag<>("1", new ArrayList<>());
        when(bagFetcher.fetch()).thenReturn(Observable.just(bag));

        final Type type = new TypeToken<Bag<String>>() {
        }.getType();

        final LiveboxBuilder<Bag<String>, Bag<String>> builder = new LiveboxBuilder<>();
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey("some_key")
                .fetch(bagFetcher, type)
                .ignoreCache(true)
                .build();

        final TestObserver<Bag<String>> bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe();
        bagBox.asObservable().subscribe(bagTestObserver);

        // Assert test observer
        bagTestObserver
                .assertSubscribed()
                .assertNoErrors()
                .assertValue(bag);

        // Verify fetcher was called two times
        verify(bagFetcher, times(2)).fetch();

    }

    /**
     * Check {@link Fetcher} instance was called one time.
     */
    @Test
    public void fetchWithDiskLruSource() {
        Livebox.init(new Config());

        // Setup mock fetcher
        @SuppressWarnings("unchecked") final Fetcher<Bag<String>> bagFetcher = (Fetcher<Bag<String>>) Mockito.mock(Fetcher.class);
        final Bag<String> bag = new Bag<>("1", new ArrayList<>());
        when(bagFetcher.fetch()).thenReturn(Observable.just(bag));

        final Type type = new TypeToken<Bag<String>>() {
        }.getType();

        final LiveboxBuilder<Bag<String>, Bag<String>> builder = new LiveboxBuilder<>();
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey("some_key")
                .fetch(bagFetcher, type)
                .addSource(Sources.MEMORY_LRU, (Validator<Bag<String>>) (key, item) -> true)
                .ignoreCache(false)
                .build();

        final TestObserver<Bag<String>> bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe();
        bagBox.asObservable().subscribe(bagTestObserver);

        // Assert test observer
        bagTestObserver
                .assertSubscribed()
                .assertNoErrors()
                .assertValue(bag);

        // Verify fetcher was called two times
        verify(bagFetcher, times(1)).fetch();
    }

}
