package com.creations.livebox;

import com.creations.livebox.datasources.fetcher.Fetcher;
import com.creations.livebox.datasources.fetcher.FileFetcher;
import com.creations.livebox_common.util.Bag;
import com.creations.livebox_common.util.Logger;
import com.creations.serializer_gson.LiveboxGsonSerializer;
import com.google.gson.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.lang.reflect.Type;

import io.reactivex.observers.TestObserver;

import static com.creations.livebox.LiveboxTest.testConfig;

/**
 * @author Sérgio Serra on 07/09/2018.
 * sergioserra99@gmail.com
 */
public class FileFetcherTests {

    private final static String TEST_KEY = "test_key";
    private final static Type TYPE = new TypeToken<Bag<String>>() {
    }.getType();

    @Before
    public void before() {
        Logger.disable();
    }

    @Test
    public void testFileFetcher() throws FileNotFoundException {
        Livebox.init(testConfig);

        final Fetcher<Bag<String>> bagFetcher =
                FileFetcher.create("src/test/resources/bag.json", TYPE, LiveboxGsonSerializer.Companion.create());

        final Box<Bag<String>, Bag<String>> builder = new Box<>(TYPE);
        Livebox<Bag<String>, Bag<String>> bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher)
                .ignoreCache(true)
                .build();

        final TestObserver<Bag<String>> bagTestObserver = new TestObserver<>();
        bagBox.asObservable().subscribe(bagTestObserver);

        bagTestObserver
                .assertSubscribed()
                .assertNoErrors()
                .assertValue(bag -> bag.getId().equals("1"));

    }

}
