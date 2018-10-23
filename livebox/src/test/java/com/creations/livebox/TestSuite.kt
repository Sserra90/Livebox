package com.creations.livebox

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
        DiskLruDataSourceTest::class,
        DiskPersistentDataSourceTest::class,
        FileFetcherTests::class,
        JournalTests::class,
        LiveboxTest::class
)
class LiveboxTestSuite