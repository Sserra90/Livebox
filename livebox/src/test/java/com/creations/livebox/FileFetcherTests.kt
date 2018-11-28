package com.creations.livebox

import com.creations.livebox.LiveboxTest.testConfig
import com.creations.livebox.datasources.fetcher.FileFetcher
import com.creations.livebox_common.util.Bag
import com.creations.livebox_common.util.Logger
import com.creations.serializer_gson.LiveboxGsonSerializer
import com.google.gson.reflect.TypeToken
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import java.io.FileNotFoundException

/**
 * @author Sérgio Serra on 07/09/2018.
 * sergioserra99@gmail.com
 */
class FileFetcherTests {

    @Before
    fun before() {
        Logger.disable()
    }

    @Test
    @Throws(FileNotFoundException::class)
    fun testFileFetcher() {
        Livebox.init(testConfig)

        val bagFetcher = FileFetcher.create<Bag<String>>(
                "src/test/resources/bag.json",
                TYPE,
                LiveboxGsonSerializer.create()
        )

        val builder = Box<Bag<String>, Bag<String>>(TYPE)
        val bagBox = builder
                .withKey(TEST_KEY)
                .fetch(bagFetcher)
                .ignoreCache(true)
                .build()

        val bagTestObserver = TestObserver<Bag<String>>()
        bagBox.asObservable().subscribe(bagTestObserver)

        bagTestObserver
                .assertSubscribed()
                .assertNoErrors()
                .assertValue { (id) -> id == "1" }

    }

    companion object {
        private const val TEST_KEY = "test_key"
        private val TYPE = object : TypeToken<Bag<String>>() {}.type
    }

}
